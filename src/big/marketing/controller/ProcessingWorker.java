package big.marketing.controller;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.apache.log4j.Logger;

import big.marketing.data.DataType;

import com.mongodb.AggregationOutput;
import com.mongodb.BasicDBObject;
import com.mongodb.DB;
import com.mongodb.DBCollection;
import com.mongodb.DBObject;

public class ProcessingWorker {
	static Logger logger = Logger.getLogger(ProcessingWorker.class);

	private final DataType type;
	private final MongoController mc;

	/**
	 * Should be "time", aggregation on database level is done for every distinct value of this field
	 */
	private final String mainFeature, prefix;
	private ArrayList<DBObject> writeBuffer;
	private List<String> groupingFields;
	private List<String> additionalFields;
	private List<String> operatorsOnAdditionalFields;

	/**
	 * Creates a new DataProcessor on the given datatype. additionalFields and
	 * groupingFields have to be added seperatly.
	 * 
	 * @param mongo for database connection
	 * @param t type of data that will be processed
	 */
	public ProcessingWorker(MongoController mongo, DataType t) {
		this(mongo, t, "time", "small");
	}

	private ProcessingWorker(MongoController mongo, DataType t, String mainFeature, String prefix) {
		this.type = t;
		this.mc = mongo;
		this.mainFeature = mainFeature;
		this.prefix = prefix;
		this.writeBuffer = new ArrayList<DBObject>(MongoController.BUFFER_SIZE);
		groupingFields = new ArrayList<String>();
		additionalFields = new ArrayList<String>();
		operatorsOnAdditionalFields = new ArrayList<String>();
		setupFields();
	}

	private void setupFields() {
		/*
		 * This sample is doing the following:
		 * Query all flow messages that have the same value in Time, SourceIP,
		 * DestIP and destinationPort (these fields are specified by _groupingFields_)
		 * This set of flow messages is combined to one single flow message and only the fields that are specified in
		 * _additionalFields_ survive for combining the different values MongoDB database commands are used (e.g $avg, $sum, ...)
		 * The resulting flow message for one set contains only the fields in _groupingFields_ and _additionalFields_
		 */
		switch (type) {
		case FLOW:
			addGroupingField("srcIP");
			addGroupingField("dstIP");
			addAdditionalField("dstPort", "$addToSet");
			addAdditionalField("srcPort", "$addToSet");
			addAdditionalField("duration", "$avg");
			addAdditionalField("payloadBytes", "$sum");
			addAdditionalField("totalBytes", "$sum");
			addAdditionalField("packetCount", "$sum");
			break;

		// TODO: decide on preprocessing for other datatypes
		case IPS:
			break;
		case HEALTH:
			addGroupingField("hostname");
			addAdditionalField("statusVal", "$max");
			addAdditionalField("diskUsage", "$avg");
			addAdditionalField("pageFileUsage", "$avg");
			addAdditionalField("numProcs", "$avg");
			addAdditionalField("loadAverage", "$avg");
			addAdditionalField("physicalMemoryUsage", "$avg");
			addAdditionalField("connMade", "$max");

			break;
		case DESCRIPTION:
			break;
		}
	}

	public void addAdditionalField(String field, String operation) {
		additionalFields.add(field);
		operatorsOnAdditionalFields.add(operation);
	}

	public void addGroupingField(String field) {
		groupingFields.add(field);
	}

	private DBObject setupGroupOperation() {
		BasicDBObject groupIDs = new BasicDBObject();
		for (String feature : groupingFields) {
			groupIDs.append(feature, "$" + feature);
		}
		BasicDBObject groupFields = new BasicDBObject("_id", groupIDs);

		for (int i = 0; i < additionalFields.size(); i++) {
			String field = additionalFields.get(i);
			String operator = operatorsOnAdditionalFields.get(i);
			groupFields.append(field, new BasicDBObject(operator, "$" + field));
		}

		DBObject groupOp = new BasicDBObject("$group", groupFields);
		return groupOp;
	}

	/**
	 * Starts processing. Resulting messages are stored in an temporary
	 * collection with name prefix+sourceCollectionName. At the end the
	 * sourceCollection is replaced by the temporary collection. Builds an index
	 * on the source collection to increase performance.
	 * 
	 * Before processing can be started, additionalFields and groupingFields have
	 * to be added!
	 * 
	 * @param prefix prefix for the name of the temporary collection.
	 */
	@SuppressWarnings("rawtypes")
	public void process() {
		DBCollection fullData = mc.getCollection(type);

		if (groupingFields.size() == 0 || additionalFields.size() == 0) {
			logger.info("No fields for processing specified, skipping data processing for " + type.name());
			createIndex(fullData);
			return;
		}

		DB db = MongoController.getDatabase();
		DBCollection smallData = db.getCollection(prefix + fullData.getName());

		// Clear target collection
		smallData.drop();

		/*
		 * Build index on complete flow data to speed up the compressing
		 * this takes some time compound index on mainFeature and groupingFields
		 * supports fast queries for mainFeature too
		 */
		BasicDBObject indexObject = new BasicDBObject(mainFeature, 1);
		for (String field : groupingFields) {
			indexObject.append(field, 1);
		}

		logger.info("Building index on full " + type.name() + " dataset ...");
		fullData.ensureIndex(indexObject);

		logger.info("Querying distinct values in field " + mainFeature);
		Collection fullDataSteps = fullData.distinct(mainFeature);
		logger.info(fullDataSteps.size() + " distinct values in field " + mainFeature);

		// Some data for progress output
		int processed = 0;
		int amount = fullDataSteps.size();
		int lastPercent = 0;
		long startTime = System.currentTimeMillis();

		logger.info("Starting data reduction for " + type.name());
		DBObject groupOp = setupGroupOperation();
		for (Object o : fullDataSteps) {
			DBObject match = new BasicDBObject(mainFeature, o);
			DBObject matchOp = new BasicDBObject("$match", match);

			AggregationOutput ao = fullData.aggregate(matchOp, groupOp);
			for (DBObject dbo : ao.results()) {

				dbo.put(mainFeature, o);
				DBObject id = (DBObject) dbo.get("_id");
				dbo.putAll(id);
				dbo.removeField("_id");

				writeBuffer.add(dbo);
				if (writeBuffer.size() >= MongoController.BUFFER_SIZE) {

					// Flush buffer to database
					smallData.insert(writeBuffer);
					writeBuffer.clear();
				}
			}

			// Some progress output
			processed++;
			int percent = processed * 100 / amount;
			// Integer operations so automatically rounded down

			if (percent > lastPercent) {
				long end = System.currentTimeMillis();
				long time = end - startTime;
				logger.info(percent + "% Done in " + time + " ms. ETA: " + ((100 - percent) * (time / percent) / 1000) + "s");
				lastPercent = percent;
			}
		}

		// Flush the rest of the buffer
		smallData.insert(writeBuffer);

		// Consistency check
		int smallDataSteps = smallData.distinct(mainFeature).size();
		if (smallDataSteps != fullDataSteps.size()) {
			logger.warn("Possible data inconsistency in " + type.name() + ":");
			logger.warn("reduced dataset contains different amount of " + mainFeature + " steps! Original dataset: " + fullDataSteps.size()
			      + " reduced dataset: " + smallDataSteps);
		}

		createIndex(smallData);
		mc.setCollection(type, smallData.getName(), false);
		logger.info("Finished data reduction for " + type.name());
	}

	private void createIndex(DBCollection collection) {
		logger.info("Creating index for " + type.name() + " on " + mainFeature);
		collection.ensureIndex(mainFeature);
	}

	public Iterable<DBObject> getAggregationQuery(String key, int min, int max) {
		DBObject match = new BasicDBObject(key, new BasicDBObject("$gt", min).append("$lt", max));
		DBObject matchOp = new BasicDBObject("$match", match);
		DBCollection fullData = mc.getCollection(type);
		DBObject groupOp = setupGroupOperation();
		AggregationOutput ao = fullData.aggregate(matchOp, groupOp);
		return ao.results();
	}
}
