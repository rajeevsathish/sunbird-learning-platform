package org.ekstep.kernel.extension;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.ekstep.kernel.extension.common.AuditProperties;
import org.ekstep.kernel.extension.common.Label;
import org.ekstep.kernel.extension.common.SystemProperties;
import org.ekstep.kernel.extension.common.TransactionEventHandlerParams;
import org.ekstep.searchindex.util.LogAsyncGraphEvent;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;
import org.neo4j.graphdb.event.LabelEntry;
import org.neo4j.graphdb.event.TransactionData;

import com.ilimi.common.dto.ExecutionContext;
import com.ilimi.common.dto.HeaderParam;

public class ProcessTransactionData {
		
	private static Logger LOGGER = LogManager.getLogger(ProcessTransactionData.class.getName());	
	protected String graphId;
	protected GraphDatabaseService graphDb;

	public ProcessTransactionData(String graphId, GraphDatabaseService graphDb) {
		this.graphId = graphId;
		this.graphDb = graphDb;
	}
	
	public void processTxnData (TransactionData data) {
		LOGGER.debug("Txn Data : " + data.toString());
		try {
		    List<Map<String, Object>> kafkaMessages = getMessageObj(data);
	        if(kafkaMessages != null && !kafkaMessages.isEmpty())
	        	LogAsyncGraphEvent.pushMessageToLogger(kafkaMessages);
		} catch (Exception e) {
		    LOGGER.error(e.getMessage(), e);
		}
	}
	
	private  String getGraphId(Node node) {
		for(org.neo4j.graphdb.Label lable :node.getLabels()){
			if(!lable.name().equals(Label.NODE.name())){
				return lable.name();
			}
		}
		return this.graphId;
	}
	
	private  String getGraphId(Iterable<LabelEntry> labels) {
		for(org.neo4j.graphdb.event.LabelEntry lable :labels){
			if(!lable.label().name().equals(Label.NODE.name())){
				return lable.label().name();
			}
		}
		return this.graphId;
	}

	private  List<Map<String, Object>> getMessageObj (TransactionData data) {
	    String userId = (String) ExecutionContext.getCurrent().getGlobalContext().get(HeaderParam.USER_ID.name());
        String requestId = (String) ExecutionContext.getCurrent().getGlobalContext().get(HeaderParam.REQUEST_ID.name());
		List<Map<String, Object>> messageMap = new ArrayList<Map<String, Object>>();
		messageMap.addAll(getCretedNodeMessages(data, graphDb, userId, requestId));
		messageMap.addAll(getUpdatedNodeMessages(data, graphDb, userId, requestId));
		messageMap.addAll(getDeletedNodeMessages(data, graphDb, userId, requestId));
		messageMap.addAll(getAddedTagsMessage(data, graphDb, userId, requestId));
		messageMap.addAll(getRemovedTagsMessage(data, graphDb, userId, requestId));
		messageMap.addAll(getAddedRelationShipMessages(data, userId, requestId));
		messageMap.addAll(getRemovedRelationShipMessages(data, userId, requestId));
		return messageMap;
	}

	private List<Map<String, Object>> getCretedNodeMessages(TransactionData data, GraphDatabaseService graphDb, String userId, String requestId) {
		List<Map<String, Object>> lstMessageMap = new ArrayList<Map<String, Object>>();
		try {
			List<Long> createdNodeIds = getCreatedNodeIds(data);
			for (Long nodeId: createdNodeIds) {
				Map<String, Object> map = new HashMap<String, Object>();
				Map<String, Object> transactionData = new HashMap<String, Object>();
				Map<String, Object> propertiesMap = getAssignedNodePropertyEntry(nodeId, data);
				if (null != propertiesMap && !propertiesMap.isEmpty()) {
				    transactionData.put(TransactionEventHandlerParams.properties.name(), propertiesMap);
			        Node node = graphDb.getNodeById(nodeId);
			        map.put(TransactionEventHandlerParams.requestId.name(), requestId);
			        if(StringUtils.isEmpty(userId)){
			            if (node.hasProperty("lastUpdatedBy"))
			                userId=(String) node.getProperty("lastUpdatedBy");
			            else
			                userId = "ANONYMOUS";
			        }
			        map.put(TransactionEventHandlerParams.userId.name(), userId);
			        map.put(TransactionEventHandlerParams.operationType.name(), TransactionEventHandlerParams.CREATE.name());
			        map.put(TransactionEventHandlerParams.label.name(), getLabel(node));
			        map.put(TransactionEventHandlerParams.graphId.name(), getGraphId(node));
			        map.put(TransactionEventHandlerParams.nodeGraphId.name(), nodeId);
			        map.put(TransactionEventHandlerParams.nodeUniqueId.name(), node.getProperty(SystemProperties.IL_UNIQUE_ID.name()));
			        if (node.hasProperty(SystemProperties.IL_FUNC_OBJECT_TYPE.name()))
			            map.put(TransactionEventHandlerParams.objectType.name(), node.getProperty(SystemProperties.IL_FUNC_OBJECT_TYPE.name()));
			        if (node.hasProperty(SystemProperties.IL_SYS_NODE_TYPE.name()))
			            map.put(TransactionEventHandlerParams.nodeType.name(), node.getProperty(SystemProperties.IL_SYS_NODE_TYPE.name()));
			        map.put(TransactionEventHandlerParams.transactionData.name(), transactionData);
			        lstMessageMap.add(map);
				}
			}
		} catch (Exception e) {
			LOGGER.error("Error building created nodes message", e);
		}
		return lstMessageMap;
	}
	
	@SuppressWarnings("unchecked")
	private List<Map<String, Object>> getUpdatedNodeMessages(TransactionData data, GraphDatabaseService graphDb, String userId, String requestId) {
		List<Map<String, Object>> lstMessageMap = new ArrayList<Map<String, Object>>();
		try {
			List<Long> updatedNodeIds = getUpdatedNodeIds(data);
			for (Long nodeId: updatedNodeIds) {
				Map<String, Object> map = new HashMap<String, Object>();
				Map<String, Object> transactionData = new HashMap<String, Object>();
				Map<String, Object> propertiesMap = getAllPropertyEntry(nodeId, data);
				if (null != propertiesMap && !propertiesMap.isEmpty()) {
				    transactionData.put(TransactionEventHandlerParams.properties.name(),getAllPropertyEntry(nodeId, data));
			        Node node = graphDb.getNodeById(nodeId);
			        map.put(TransactionEventHandlerParams.requestId.name(), requestId);
			        if(StringUtils.isEmpty(userId)){
			        	Object objUserId = null;
			            if (propertiesMap.containsKey("lastUpdatedBy")) {
			            	Object prop = propertiesMap.get("lastUpdatedBy");
			            	if (null != prop) {
			            		Map<String, Object> valueMap = (Map<String, Object>) prop;
			            		objUserId = valueMap.get("nv");
			            	}
			            }
			            if (null != objUserId)
		                    userId = objUserId.toString();
		                else
		                    userId = "ANONYMOUS";
			        }
			        map.put(TransactionEventHandlerParams.userId.name(), userId);
			        map.put(TransactionEventHandlerParams.operationType.name(), TransactionEventHandlerParams.UPDATE.name());
			        map.put(TransactionEventHandlerParams.label.name(), getLabel(node));
			        map.put(TransactionEventHandlerParams.graphId.name(), getGraphId(node));
			        map.put(TransactionEventHandlerParams.nodeGraphId.name(), nodeId);
			        map.put(TransactionEventHandlerParams.nodeUniqueId.name(), node.getProperty(SystemProperties.IL_UNIQUE_ID.name()));
			        map.put(TransactionEventHandlerParams.nodeType.name(), node.getProperty(SystemProperties.IL_SYS_NODE_TYPE.name()));
			        if (node.hasProperty(SystemProperties.IL_FUNC_OBJECT_TYPE.name()))
			            map.put(TransactionEventHandlerParams.objectType.name(), node.getProperty(SystemProperties.IL_FUNC_OBJECT_TYPE.name()));
			        map.put(TransactionEventHandlerParams.transactionData.name(), transactionData);
			        lstMessageMap.add(map);
				}
			}
		} catch (Exception e) {
			LOGGER.error("Error building updated nodes message", e);
		}
		return lstMessageMap;
	}
	
	@SuppressWarnings("rawtypes")
    private List<Map<String, Object>> getDeletedNodeMessages(TransactionData data, GraphDatabaseService graphDb, String userId, String requestId) {
		List<Map<String, Object>> lstMessageMap = new ArrayList<Map<String, Object>>();
		try {
			List<Long> deletedNodeIds = getDeletedNodeIds(data);
			for (Long nodeId: deletedNodeIds) {
				Map<String, Object> map = new HashMap<String, Object>();
				Map<String, Object> transactionData = new HashMap<String, Object>();
				Map<String, Object> removedNodeProp = getRemovedNodePropertyEntry(nodeId, data);
			    if (null != removedNodeProp && !removedNodeProp.isEmpty()) {
			        transactionData.put(TransactionEventHandlerParams.properties.name(),removedNodeProp);
			        map.put(TransactionEventHandlerParams.requestId.name(), requestId);
			        if(StringUtils.isEmpty(userId)){
			            if(removedNodeProp.containsKey("lastUpdatedBy"))
			                userId=(String)((Map)removedNodeProp.get("lastUpdatedBy")).get("ov");//oldvalue of lastUpdatedBy from the transaction data as node is deleted
			            else
			                userId = "ANONYMOUS";
			        }
			        map.put(TransactionEventHandlerParams.userId.name(), userId);
			        map.put(TransactionEventHandlerParams.operationType.name(), TransactionEventHandlerParams.DELETE.name());
			        map.put(TransactionEventHandlerParams.label.name(), getLabel(removedNodeProp));
			        map.put(TransactionEventHandlerParams.graphId.name(), getGraphId(data.removedLabels()));
			        map.put(TransactionEventHandlerParams.nodeGraphId.name(), nodeId);
			        map.put(TransactionEventHandlerParams.nodeUniqueId.name(), ((Map)removedNodeProp.get(SystemProperties.IL_UNIQUE_ID.name())).get("ov"));
			        map.put(TransactionEventHandlerParams.objectType.name(), ((Map)removedNodeProp.get(SystemProperties.IL_FUNC_OBJECT_TYPE.name())).get("ov"));
			        map.put(TransactionEventHandlerParams.nodeType.name(), ((Map)removedNodeProp.get(SystemProperties.IL_SYS_NODE_TYPE.name())).get("ov"));
			        map.put(TransactionEventHandlerParams.transactionData.name(), transactionData);
			        lstMessageMap.add(map);
			    }
			}
		} catch (Exception e) {
			LOGGER.error("Error building deleted nodes message", e);
		}
		return lstMessageMap;
	}
	
	private Map<String, Object> getAllPropertyEntry(Long nodeId, TransactionData data){
		Map<String, Object> map = getAssignedNodePropertyEntry(nodeId, data);
		map.putAll(getRemovedNodePropertyEntry(nodeId, data));
		return map;
	}
	
	private Map<String, Object> getAssignedNodePropertyEntry(Long nodeId, TransactionData data) {		
		Iterable<org.neo4j.graphdb.event.PropertyEntry<Node>> assignedNodeProp = data.assignedNodeProperties();
		return getNodePropertyEntry(nodeId, assignedNodeProp);
	}
	
	private Map<String, Object> getRemovedNodePropertyEntry(Long nodeId, TransactionData data) {
		Iterable<org.neo4j.graphdb.event.PropertyEntry<Node>> removedNodeProp = data.removedNodeProperties();
		return getNodeRemovedPropertyEntry(nodeId, removedNodeProp);
	}
	
	private Map<String, Object> getNodePropertyEntry(Long nodeId, Iterable<org.neo4j.graphdb.event.PropertyEntry<Node>> nodeProp){
		Map<String, Object> map = new HashMap<String, Object>();
		for (org.neo4j.graphdb.event.PropertyEntry<Node> pe: nodeProp) {
			if (nodeId == pe.entity().getId()) {
			    if (!compareValues(pe.previouslyCommitedValue(), pe.value())) {
			        Map<String, Object> valueMap=new HashMap<String, Object>();
	                valueMap.put("ov", pe.previouslyCommitedValue()); // old value
	                valueMap.put("nv", pe.value()); // new value
	                map.put((String) pe.key(), valueMap);
			    }
			}
		}
		if (map.size() == 1 && null != map.get(AuditProperties.lastUpdatedOn.name()))
		    map = new HashMap<String, Object>();
		return map;
	}
	
	private Map<String, Object> getNodeRemovedPropertyEntry(Long nodeId,
			Iterable<org.neo4j.graphdb.event.PropertyEntry<Node>> nodeProp) {
		Map<String, Object> map = new HashMap<String, Object>();
		for (org.neo4j.graphdb.event.PropertyEntry<Node> pe : nodeProp) {
			if (nodeId == pe.entity().getId()) {
				Map<String, Object> valueMap = new HashMap<String, Object>();
				valueMap.put("ov", pe.previouslyCommitedValue()); // old value
				valueMap.put("nv",null); // new value
				map.put((String) pe.key(), valueMap);
			}
		}
		if (map.size() == 1 && null != map.get(AuditProperties.lastUpdatedOn.name()))
			map = new HashMap<String, Object>();
		return map;
	}
	
	@SuppressWarnings("rawtypes")
    private boolean compareValues(Object o1, Object o2) {
	    if (null == o1)
	        o1 = "";
	    if (null == o2)
            o2 = "";
	    if (o1.equals(o2))
	        return true;
	    else {
	        if (o1 instanceof List) {
	            if (!(o2 instanceof List))
	                return false;
	            else
	                return compareLists((List) o1, (List) o2);
	        } else if (o1 instanceof Object[]) {
	            if (!(o2 instanceof Object[]))
                    return false;
	            else
	                return compareArrays((Object[]) o1, (Object[]) o2);
	        }
	    }
	    return false;
	}
	
	@SuppressWarnings("rawtypes")
    private boolean compareLists(List l1, List l2) {
	    if (l1.size() != l2.size())
            return false;
	    for (int i=0; i<l1.size(); i++) {
	        Object v1 = l1.get(i);
	        Object v2 = l2.get(i);
            if ((null == v1 && null != v2) || (null != v1 && null == v2))
                return false;
            if (null != v1 && null != v2 && !v1.equals(v2))
                return false;
        }
	    return true;
	}
	
    private boolean compareArrays(Object[] l1, Object[] l2) {
        if (l1.length != l2.length)
            return false;
        for (int i=0; i<l1.length; i++) {
            Object v1 = l1[i];
            Object v2 = l2[i];
            if ((null == v1 && null != v2) || (null != v1 && null == v2))
                return false;
            if (null != v1 && null != v2 && !v1.equals(v2))
                return false;
        }
        return true;
    }
	
	private List<Map<String, Object>> getAddedTagsMessage(TransactionData data, GraphDatabaseService graphDb, String userId, String requestId) {
        List<Map<String, Object>> lstMessageMap = new ArrayList<Map<String, Object>>();
        try {
			Iterable<Relationship> createdRelations = data.createdRelationships();
			if (null != createdRelations) {
			    for (Relationship rel: createdRelations) {
			        if (StringUtils.equalsIgnoreCase(
			                    rel.getStartNode().getProperty(SystemProperties.IL_SYS_NODE_TYPE.name()).toString(), 
			                    TransactionEventHandlerParams.TAG.name())) {
			            if (rel.getStartNode().hasProperty(SystemProperties.IL_TAG_NAME.name())) {
			                Map<String, Object> transactionData = new HashMap<String, Object>();
			                List<String> tags = new ArrayList<String>();
			                transactionData.put(TransactionEventHandlerParams.properties.name(), new HashMap<String, Object>());
			                transactionData.put(TransactionEventHandlerParams.removedTags.name(), new ArrayList<String>());
			                tags.add(rel.getStartNode().getProperty(SystemProperties.IL_TAG_NAME.name()).toString());
			                transactionData.put(TransactionEventHandlerParams.addedTags.name(), tags);
			                Node node = graphDb.getNodeById(rel.getEndNode().getId());
			                Map<String, Object> map = new HashMap<String, Object>();
			                if(StringUtils.isEmpty(userId)){
			                    if (node.hasProperty("lastUpdatedBy"))
			                        userId=(String) node.getProperty("lastUpdatedBy");
			                    else
			                        userId = "ANONYMOUS";
			                }
			                map.put(TransactionEventHandlerParams.requestId.name(), requestId);
			                map.put(TransactionEventHandlerParams.userId.name(), userId);
			                map.put(TransactionEventHandlerParams.operationType.name(), TransactionEventHandlerParams.UPDATE.name());
			                map.put(TransactionEventHandlerParams.label.name(), getLabel(node));
			                map.put(TransactionEventHandlerParams.graphId.name(), getGraphId(node));
			                map.put(TransactionEventHandlerParams.nodeGraphId.name(), node.getId());
			                map.put(TransactionEventHandlerParams.nodeUniqueId.name(), node.getProperty(SystemProperties.IL_UNIQUE_ID.name()));
			                if (node.hasProperty(SystemProperties.IL_FUNC_OBJECT_TYPE.name()))
			                    map.put(TransactionEventHandlerParams.objectType.name(), node.getProperty(SystemProperties.IL_FUNC_OBJECT_TYPE.name()));
			                map.put(TransactionEventHandlerParams.nodeType.name(), node.getProperty(SystemProperties.IL_SYS_NODE_TYPE.name()));
			                map.put(TransactionEventHandlerParams.transactionData.name(), transactionData);
			                lstMessageMap.add(map);
			            }
			        }
			    }
			}
		} catch (Exception e) {
			LOGGER.error("Error building added tags message", e);
		}
        return lstMessageMap;
    }
	
	private List<Map<String, Object>> getRemovedTagsMessage(TransactionData data, GraphDatabaseService graphDb, String userId, String requestId) {
        List<Map<String, Object>> lstMessageMap = new ArrayList<Map<String, Object>>();
        try {
			Iterable<Relationship> createdRelations = data.deletedRelationships();
			if (null != createdRelations) {
			    for (Relationship rel: createdRelations) {
			        if (rel.getStartNode().hasProperty(SystemProperties.IL_SYS_NODE_TYPE.name()) && StringUtils.equalsIgnoreCase(
			                    rel.getStartNode().getProperty(SystemProperties.IL_SYS_NODE_TYPE.name()).toString(), 
			                    TransactionEventHandlerParams.TAG.name())) {
			            if (rel.getStartNode().hasProperty(SystemProperties.IL_TAG_NAME.name())) {
			                Map<String, Object> transactionData = new HashMap<String, Object>();
			                List<String> tags = new ArrayList<String>();
			                transactionData.put(TransactionEventHandlerParams.properties.name(), new HashMap<String, Object>());
			                transactionData.put(TransactionEventHandlerParams.addedTags.name(), new ArrayList<String>());
			                tags.add(rel.getStartNode().getProperty(SystemProperties.IL_TAG_NAME.name()).toString());
			                transactionData.put(TransactionEventHandlerParams.removedTags.name(), tags);
			                Node node = graphDb.getNodeById(rel.getEndNode().getId());
			                Map<String, Object> map = new HashMap<String, Object>();
			                if(StringUtils.isEmpty(userId)){
			                    if (node.hasProperty("lastUpdatedBy"))
			                        userId=(String) node.getProperty("lastUpdatedBy");
			                    else
			                        userId = "ANONYMOUS";
			                }
			                map.put(TransactionEventHandlerParams.requestId.name(), requestId);
			                map.put(TransactionEventHandlerParams.userId.name(), userId);
			                map.put(TransactionEventHandlerParams.operationType.name(), TransactionEventHandlerParams.UPDATE.name());
			                map.put(TransactionEventHandlerParams.label.name(), getLabel(node));
			                map.put(TransactionEventHandlerParams.graphId.name(), getGraphId(node));
			                map.put(TransactionEventHandlerParams.nodeGraphId.name(), node.getId());
			                map.put(TransactionEventHandlerParams.nodeUniqueId.name(), node.getProperty(SystemProperties.IL_UNIQUE_ID.name()));
			                if (node.hasProperty(SystemProperties.IL_FUNC_OBJECT_TYPE.name()))
			                    map.put(TransactionEventHandlerParams.objectType.name(), node.getProperty(SystemProperties.IL_FUNC_OBJECT_TYPE.name()));
			                map.put(TransactionEventHandlerParams.nodeType.name(), node.getProperty(SystemProperties.IL_SYS_NODE_TYPE.name()));
			                map.put(TransactionEventHandlerParams.transactionData.name(), transactionData);
			                lstMessageMap.add(map);
			            }
			        }
			    }
			}
		} catch (Exception e) {
			LOGGER.error("Error building removed tags message", e);
		}
        return lstMessageMap;
    }
	
	private List<Map<String, Object>> getAddedRelationShipMessages(TransactionData data, String userId, String requestId) {
		Iterable<Relationship> createdRelations = data.createdRelationships();
		return getRelationShipMessages(createdRelations, TransactionEventHandlerParams.UPDATE.name(), false, userId, requestId);
	}
	
	private List<Map<String, Object>> getRemovedRelationShipMessages(TransactionData data, String userId, String requestId) {
		Iterable<Relationship> deletedRelations = data.deletedRelationships();
		return getRelationShipMessages(deletedRelations, TransactionEventHandlerParams.UPDATE.name(), true, userId, requestId);
	}
	
    private List<Map<String, Object>> getRelationShipMessages(Iterable<Relationship> relations, String operationType, boolean delete, String userId, String requestId) {
		List<Map<String, Object>> lstMessageMap = new ArrayList<Map<String, Object>>();
		try {
			if (null != relations) {
			    for (Relationship rel: relations) {
			        Node startNode = rel.getStartNode();
			        Node endNode =  rel.getEndNode();
			        String relationTypeName=rel.getType().name();
			        if(StringUtils.equalsIgnoreCase(startNode.getProperty(SystemProperties.IL_SYS_NODE_TYPE.name()).toString(), 
			                TransactionEventHandlerParams.TAG.name()))
			            continue;

			        //start_node message 
			        Map<String, Object> map = new HashMap<String, Object>();
			        Map<String, Object> transactionData = new HashMap<String, Object>();
			        Map<String, Object> startRelation = new HashMap<>();        

			        startRelation.put("rel", relationTypeName);
			        startRelation.put("id", endNode.getProperty(SystemProperties.IL_UNIQUE_ID.name()));
			        startRelation.put("dir", "OUT");
			        if (endNode.hasProperty(SystemProperties.IL_FUNC_OBJECT_TYPE.name()))
			            startRelation.put("type", endNode.getProperty(SystemProperties.IL_FUNC_OBJECT_TYPE.name()));
			        startRelation.put("label", getLabel(endNode));
			        
			        if(StringUtils.isEmpty(userId)){
			            String startNodeLastUpdate = (String) getPropertyValue(startNode , "lastUpdatedOn");
			            String endNodeLastUpdate = (String) getPropertyValue(endNode ,"lastUpdatedOn");
			            
			            if(startNodeLastUpdate != null && endNodeLastUpdate != null){
			                if(startNodeLastUpdate.compareTo(endNodeLastUpdate)>0){
			                    userId=(String) getPropertyValue(startNode ,"lastUpdatedBy");
			                }else{
			                    userId=(String) getPropertyValue(endNode ,"lastUpdatedBy");                     
			                }
			            }
			            if(StringUtils.isBlank(userId))
			                userId = "ANONYMOUS";
			        }
			        List<Map<String, Object>> startRelations = new ArrayList<Map<String, Object>>();
			        startRelations.add(startRelation);
			        transactionData.put(TransactionEventHandlerParams.properties.name(), new HashMap<String, Object>());
			        transactionData.put(TransactionEventHandlerParams.removedTags.name(), new ArrayList<String>());
			        transactionData.put(TransactionEventHandlerParams.addedTags.name(), new ArrayList<String>());
			        if (delete) {
			            transactionData.put(TransactionEventHandlerParams.removedRelations.name(), startRelations);
			            transactionData.put(TransactionEventHandlerParams.addedRelations.name(), new ArrayList<Map<String, Object>>());
			        } else {
			            transactionData.put(TransactionEventHandlerParams.addedRelations.name(), startRelations);
			            transactionData.put(TransactionEventHandlerParams.removedRelations.name(), new ArrayList<Map<String, Object>>());
			        }
			        map.put(TransactionEventHandlerParams.requestId.name(), requestId);
			        map.put(TransactionEventHandlerParams.userId.name(), userId);
			        map.put(TransactionEventHandlerParams.operationType.name(), operationType);
			        map.put(TransactionEventHandlerParams.label.name(), getLabel(startNode));
			        map.put(TransactionEventHandlerParams.graphId.name(), getGraphId(startNode));
			        map.put(TransactionEventHandlerParams.nodeGraphId.name(), startNode.getId());
			        map.put(TransactionEventHandlerParams.nodeUniqueId.name(), startNode.getProperty(SystemProperties.IL_UNIQUE_ID.name()));
			        map.put(TransactionEventHandlerParams.nodeType.name(), startNode.getProperty(SystemProperties.IL_SYS_NODE_TYPE.name()));           
			        if (startNode.hasProperty(SystemProperties.IL_FUNC_OBJECT_TYPE.name()))
			            map.put(TransactionEventHandlerParams.objectType.name(), startNode.getProperty(SystemProperties.IL_FUNC_OBJECT_TYPE.name()));

			        map.put(TransactionEventHandlerParams.transactionData.name(), transactionData);
			        lstMessageMap.add(map);
			        
			        //end_node message 
			        map = new HashMap<String, Object>();
			        transactionData = new HashMap<String, Object>();
			        Map<String, Object> endRelation = new HashMap<>();      

			        endRelation.put("rel", relationTypeName);
			        endRelation.put("id", startNode.getProperty(SystemProperties.IL_UNIQUE_ID.name()));
			        endRelation.put("dir", "IN");
			        if (startNode.hasProperty(SystemProperties.IL_FUNC_OBJECT_TYPE.name()))
			            endRelation.put("type", startNode.getProperty(SystemProperties.IL_FUNC_OBJECT_TYPE.name()));
			        endRelation.put("label", getLabel(startNode));
			        List<Map<String, Object>> endRelations = new ArrayList<Map<String, Object>>();
			        endRelations.add(endRelation);
			        transactionData.put(TransactionEventHandlerParams.properties.name(), new HashMap<String, Object>());
			        transactionData.put(TransactionEventHandlerParams.removedTags.name(), new ArrayList<String>());
			        transactionData.put(TransactionEventHandlerParams.addedTags.name(), new ArrayList<String>());
			        if (delete) {
			            transactionData.put(TransactionEventHandlerParams.removedRelations.name(), endRelations);
			            transactionData.put(TransactionEventHandlerParams.addedRelations.name(), new ArrayList<Map<String, Object>>());
			        } else {
			            transactionData.put(TransactionEventHandlerParams.addedRelations.name(), endRelations);
			            transactionData.put(TransactionEventHandlerParams.removedRelations.name(), new ArrayList<Map<String, Object>>());
			        }
			        map.put(TransactionEventHandlerParams.requestId.name(), requestId);            
			        map.put(TransactionEventHandlerParams.userId.name(), userId);
			        map.put(TransactionEventHandlerParams.operationType.name(), operationType);
			        map.put(TransactionEventHandlerParams.label.name(), getLabel(endNode));
			        map.put(TransactionEventHandlerParams.graphId.name(), getGraphId(endNode));
			        map.put(TransactionEventHandlerParams.nodeGraphId.name(), endNode.getId());
			        map.put(TransactionEventHandlerParams.nodeUniqueId.name(), endNode.getProperty(SystemProperties.IL_UNIQUE_ID.name()));
			        map.put(TransactionEventHandlerParams.nodeType.name(), endNode.getProperty(SystemProperties.IL_SYS_NODE_TYPE.name()));         
			        if (startNode.hasProperty(SystemProperties.IL_FUNC_OBJECT_TYPE.name()))
			            map.put(TransactionEventHandlerParams.objectType.name(), endNode.getProperty(SystemProperties.IL_FUNC_OBJECT_TYPE.name()));

			        map.put(TransactionEventHandlerParams.transactionData.name(), transactionData);
			        lstMessageMap.add(map);
			    }
			}
		} catch (Exception e) {
			LOGGER.error("Error building updated relations message", e);
		}
		return lstMessageMap;
	}
	
	private String getLabel(Node node){
		if(node.hasProperty("name")){
			return (String) node.getProperty("name");
		}else if(node.hasProperty("lemma")){
			return (String) node.getProperty("lemma");
		}else if(node.hasProperty("title")){
			return (String) node.getProperty("title");
		}else if(node.hasProperty("gloss")){
			return (String) node.getProperty("gloss");
		}
		return "";
	}

	@SuppressWarnings("rawtypes")
    private String getLabel(Map<String, Object> nodeMap){
		if(nodeMap.containsKey("name")){
			return (String) ((Map)nodeMap.get("name")).get("ov");
		}else if(nodeMap.containsKey("lemma")){
			return (String) ((Map)nodeMap.get("lemma")).get("ov");
		}else if(nodeMap.containsKey("title")){
			return (String) ((Map)nodeMap.get("title")).get("ov");
		}else if(nodeMap.containsKey("gloss")){
			return (String) ((Map)nodeMap.get("gloss")).get("ov");
		}
		
		return "";
	}
	private Object getPropertyValue(Node node , String propertyName){
		if (node.hasProperty(propertyName))
			return node.getProperty(propertyName);
		return null;
	}
	
	private List<Long> getUpdatedNodeIds(TransactionData data) {
		List<Long> lstNodeIds = new ArrayList<Long>();
		List<Long> lstCreatedNodeIds = getCreatedNodeIds(data);
		List<Long> lstDeletedNodeIds = getDeletedNodeIds(data);
		Iterable<org.neo4j.graphdb.event.PropertyEntry<Node>> assignedNodeProp = data.assignedNodeProperties();
		for (org.neo4j.graphdb.event.PropertyEntry<Node> pe: assignedNodeProp) {
			if (!lstCreatedNodeIds.contains(pe.entity().getId()) &&
					!lstDeletedNodeIds.contains(pe.entity().getId())) {
				lstNodeIds.add(pe.entity().getId());
			}
		}
		Iterable<org.neo4j.graphdb.event.PropertyEntry<Node>> removedNodeProp = data.removedNodeProperties();
		for (org.neo4j.graphdb.event.PropertyEntry<Node> pe: removedNodeProp) {
			if (!lstCreatedNodeIds.contains(pe.entity().getId()) &&
					!lstDeletedNodeIds.contains(pe.entity().getId())) {
				lstNodeIds.add(pe.entity().getId());
			}
		}
		return new ArrayList<Long>(new HashSet<Long>(lstNodeIds));
	}

	private List<Long> getCreatedNodeIds(TransactionData data) {
		List<Long> lstNodeIds = new ArrayList<Long>();
		if (null != data.createdNodes()) {
            Iterator<Node> nodes =  data.createdNodes().iterator();
            while (nodes.hasNext()) {
            	lstNodeIds.add(nodes.next().getId());
            }
        }
		
		return new ArrayList<Long>(new HashSet<Long>(lstNodeIds));
	}

	private List<Long> getDeletedNodeIds(TransactionData data) {
		List<Long> lstNodeIds = new ArrayList<Long>();
		if (null != data.deletedNodes()) {
            Iterator<Node> nodes =  data.deletedNodes().iterator();
            while (nodes.hasNext()) {
            	lstNodeIds.add(nodes.next().getId());
            }
        }
		
		return new ArrayList<Long>(new HashSet<Long>(lstNodeIds));
	}
}