package org.ekstep.searchindex.processor;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.codehaus.jackson.JsonGenerationException;
import org.codehaus.jackson.map.JsonMappingException;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.type.TypeReference;

import com.ilimi.common.logger.LogHelper;
import com.ilimi.dac.dto.AuditHistoryRecord;
import com.ilimi.taxonomy.mgr.IAuditHistoryManager;
import com.ilimi.util.ApplicationContextUtils;

/**
 * The Class AuditHistoryMessageProcessor provides implementations of the core
 * operations defined in the IMessageProcessor along with the methods to
 * getAuditLogs and their properties
 * 
 * @author Karthik, Rashmi
 * 
 * @see IMessageProcessor
 */
public class AuditHistoryMessageProcessor implements IMessageProcessor {

	/** The LOGGER */
	private static LogHelper LOGGER = LogHelper.getInstance(AuditHistoryMessageProcessor.class.getName());

	/** The ObjectMapper */
	private ObjectMapper mapper = new ObjectMapper();

	/** The interface IAduitHistoryManager */
	private IAuditHistoryManager manager = null;

	/** The constructor */
	public AuditHistoryMessageProcessor() {
		super();
	}
	
	/*
	 * (non-Javadoc)
	 * 
	 * @see org.ekstep.searchindex.processor #processMessage(java.lang.String,
	 * java.lang.String, java.io.File, java.lang.String)
	 */
	@Override
	public void processMessage(String messageData) {
		try {
			Map<String, Object> message = mapper.readValue(messageData, new TypeReference<Map<String, Object>>() {
			});
			if (null != message)
				processMessage(message);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.ekstep.searchindex.processor #processMessage(java.lang.String,
	 * java.lang.String, java.io.File, java.lang.String)
	 */
	@Override
	public void processMessage(Map<String, Object> message) throws Exception {
		if (null == manager) {
			manager = (IAuditHistoryManager) ApplicationContextUtils.getApplicationContext()
					.getBean("auditHistoryManager");
		}
		LOGGER.info("Processing audit history message: Object Type: " + message.get("objectType") + " | Identifier: "
				+ message.get("nodeUniqueId") + " | Graph: " + message.get("graphId") + " | Operation: "
				+ message.get("operationType"));
		if (message != null && message.get("operationType") != null && null == message.get("syncMessage")) {
			AuditHistoryRecord record = getAuditHistory(message);
			manager.saveAuditHistory(record);
		}
	}

	/** 
	 * This method getAuditHistory sets the required data from the transaction message 
	 * that can be saved to mysql DB
	 * 
	 * @param transactionDataMap
	 *        The Neo4j TransactionDataMap
	 *        
	 * @return AuditHistoryRecord that can be saved to mysql DB
	 */
	private AuditHistoryRecord getAuditHistory(Map<String, Object> transactionDataMap) {
		AuditHistoryRecord record = new AuditHistoryRecord();

		try {
			record.setUserId((String) transactionDataMap.get("userId"));
			record.setRequestId((String) transactionDataMap.get("requestId"));
			record.setObjectId((String) transactionDataMap.get("nodeUniqueId"));
			record.setObjectType((String) transactionDataMap.get("objectType"));
			record.setGraphId((String) transactionDataMap.get("graphId"));
			record.setOperation((String) transactionDataMap.get("operationType"));
			record.setLabel((String) transactionDataMap.get("label"));
			String transactionDataStr = mapper.writeValueAsString(transactionDataMap.get("transactionData"));
			record.setLogRecord(transactionDataStr);
			String summary = setSummaryData(transactionDataStr);
			record.setSummary(summary);
			record.setCreatedOn(new Date());
		} catch (IOException e) {
			e.printStackTrace();
		}
		return record;
	}

	/** 
	 * This method setSummaryData sets the required summaryData from the transaction message 
	 * and that can be saved to mysql DB
	 * 
	 * @param transactionDataMap
	 *        The Neo4j TransactionDataMap
	 *        
	 * @return summary
	 */
	@SuppressWarnings({ "unchecked", "rawtypes" })
	public String setSummaryData(String transactionDataStr) {

		Map<String, Object> summaryData = new HashMap<String, Object>();
		Map<String, Integer> relations = new HashMap<String, Integer>();
		Map<String, Integer> tags = new HashMap<String, Integer>();
		Map<String, Object> properties = new HashMap<String, Object>();

		List<String> fields = new ArrayList<String>();
		TypeReference<HashMap<String, Object>> typeRef = new TypeReference<HashMap<String, Object>>() {
		};
		HashMap<String, Object> transactionMap;
		String summaryResult = null;
		try {
			transactionMap = mapper.readValue(transactionDataStr, typeRef);
			for (Map.Entry<String, Object> entry : transactionMap.entrySet()) {

				if (entry.getKey().equals("addedRelations")) {
					List<Object> list = (List) entry.getValue();
					if (!list.isEmpty()) {

						relations.put("addedRelations", list.size());
					} else {
						relations.put("addedRelations", 0);
					}
					summaryData.put("relations", relations);

				} else if (entry.getKey().equals("removedRelations")) {
					List<Object> list = (List) entry.getValue();
					if (!list.isEmpty()) {
						relations.put("removedRelations", list.size());
					} else {
						relations.put("removedRelations", 0);
					}
					summaryData.put("relations", relations);

				} else if (entry.getKey().equals("addedTags")) {
					List<Object> list = (List) entry.getValue();
					if (!list.isEmpty()) {
						list.add(entry.getValue());
						tags.put("addedTags", list.size());
					} else {
						tags.put("addedTags", 0);
					}
					summaryData.put("tags", tags);

				} else if (entry.getKey().equals("removedTags")) {
					List<Object> list = (List) entry.getValue();
					if (!list.isEmpty()) {
						list.add(entry.getValue());
						tags.put("removedTags", list.size());
					} else {
						tags.put("removedTags", 0);
					}
					summaryData.put("tags", tags);

				} else if (entry.getKey().equals("properties")) {
					if (!entry.getValue().toString().isEmpty()) {
						String props = mapper.writeValueAsString(entry.getValue());
						HashMap<String, Object> propsMap = mapper.readValue(props, typeRef);

						Set<String> propertiesSet = propsMap.keySet();
						for (String s : propertiesSet) {
							fields.add(s);
						}
					}
					properties.put("count", fields.size());
					properties.put("fields", fields);
					summaryData.put("properties", properties);
				}
			}
		     summaryResult = mapper.writeValueAsString(summaryData);
			
		} catch (IOException e) {
			e.printStackTrace();
		}
		return summaryResult;
	}
}