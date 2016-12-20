package com.ilimi.assessment.controller;

import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import com.ilimi.assessment.dto.ItemSetSearchCriteria;
import com.ilimi.assessment.enums.AssessmentAPIParams;
import com.ilimi.assessment.enums.AssessmentErrorCodes;
import com.ilimi.assessment.mgr.IAssessmentManager;
import com.ilimi.common.controller.BaseController;
import com.ilimi.common.dto.Request;
import com.ilimi.common.dto.Response;
import com.ilimi.common.exception.MiddlewareException;
import com.ilimi.common.logger.LogHelper;
import com.ilimi.graph.dac.model.Node;

/**
 * 
 * @author mahesh
 *
 */

@Controller
@RequestMapping("/v3")
public class AssessmentItemSetV3Controller extends BaseController {

    private static LogHelper LOGGER = LogHelper.getInstance(AssessmentItemSetV3Controller.class.getName());

    private static final String V2_GRAPH_ID = "domain";

    @Autowired
    private IAssessmentManager assessmentManager;

    @RequestMapping(value = "/private/assessment/itemset/create", method = RequestMethod.POST)
    @ResponseBody
    public ResponseEntity<Response> create(
            @RequestBody Map<String, Object> map, @RequestHeader(value = "user-id") String userId) {
    	String taxonomyId = V2_GRAPH_ID;
        String apiId = "assessment_item_set.create";
        Request request = getRequestObject(map);
        LOGGER.info("Create | TaxonomyId: " + taxonomyId + " | Request: " + request + " | user-id: " + userId);
        try {
            Response response = assessmentManager.createItemSet(taxonomyId, request);
            LOGGER.info("Create | Response: " + response);
            return getResponseEntity(response, apiId,
                    (null != request.getParams()) ? request.getParams().getMsgid() : null);
        } catch (Exception e) {
            LOGGER.error("Create | Exception: " + e.getMessage(), e);
            return getExceptionResponseEntity(e, apiId,
                    (null != request.getParams()) ? request.getParams().getMsgid() : null);
        }
    }

    @RequestMapping(value = "/private/assessment/itemset/update/{id:.+}", method = RequestMethod.PATCH)
    @ResponseBody
    public ResponseEntity<Response> update(@PathVariable(value = "id") String id,
            @RequestBody Map<String, Object> map, @RequestHeader(value = "user-id") String userId) {
    	String taxonomyId = V2_GRAPH_ID;
        String apiId = "assessment_item_set.update";
        Request request = getRequestObject(map);
        LOGGER.info("Update Item | TaxonomyId: " + taxonomyId + " | Id: " + id + " | Request: " + request
                + " | user-id: " + userId);
        try {
            Response response = assessmentManager.updateItemSet(id, taxonomyId, request);
            LOGGER.info("Update | Response: " + response);
            return getResponseEntity(response, apiId,
                    (null != request.getParams()) ? request.getParams().getMsgid() : null);
        } catch (Exception e) {
            LOGGER.error("Update | Exception: " + e.getMessage(), e);
            return getExceptionResponseEntity(e, apiId,
                    (null != request.getParams()) ? request.getParams().getMsgid() : null);
        }
    }

    @RequestMapping(value = "/public/assessment/itemset/read/{id:.+}", method = RequestMethod.GET)
    @ResponseBody
    public ResponseEntity<Response> find(@PathVariable(value = "id") String id,
            @RequestParam(value = "isfields", required = false) String[] isfields,
            @RequestHeader(value = "user-id") String userId) {
    	String taxonomyId = V2_GRAPH_ID;
        String apiId = "assessment_item_set.find";
        LOGGER.info("Find | TaxonomyId: " + taxonomyId + " | Id: " + id + " | ifields: " + isfields + " | user-id: "
                + userId);
        try {
            Response response = assessmentManager.getItemSet(id, taxonomyId, isfields, false);
            LOGGER.info("Find | Response: " + response);
            return getResponseEntity(response, apiId, null);
        } catch (Exception e) {
            LOGGER.error("Find | Exception: " + e.getMessage(), e);
            return getExceptionResponseEntity(e, apiId, null);
        }
    }

    @RequestMapping(value = "/public/assessment/itemset/generate/{id:.+}", method = RequestMethod.GET)
    @ResponseBody
    public ResponseEntity<Response> generate(@PathVariable(value = "id") String id,
            @RequestParam(value = "isfields", required = false) String[] isfields,
            @RequestHeader(value = "user-id") String userId) {
    	String taxonomyId = V2_GRAPH_ID;
        String apiId = "assessment_item_set.find";
        LOGGER.info("Find | TaxonomyId: " + taxonomyId + " | Id: " + id + " | ifields: " + isfields + " | user-id: "
                + userId);
        try {
            Response response = assessmentManager.getItemSet(id, taxonomyId, isfields, true);
            LOGGER.info("Find | Response: " + response);
            return getResponseEntity(response, apiId, null);
        } catch (Exception e) {
            LOGGER.error("Find | Exception: " + e.getMessage(), e);
            return getExceptionResponseEntity(e, apiId, null);
        }
    }

    @RequestMapping(value = "/public/assessment/itemset/search", method = RequestMethod.POST)
    @ResponseBody
    public ResponseEntity<Response> search(
            @RequestBody Map<String, Object> map, @RequestHeader(value = "user-id") String userId) {
    	String taxonomyId = V2_GRAPH_ID;
        String apiId = "assessment_item_set.search";
        LOGGER.info("Search | TaxonomyId: " + taxonomyId + " | user-id: " + userId);
        try {
            Request reqeust = getSearchRequest(map);
            Response response = assessmentManager.searchItemSets(taxonomyId, reqeust);
            LOGGER.info("Search | Response: " + response);
            return getResponseEntity(response, apiId, null);
        } catch (Exception e) {
            LOGGER.error("Search | Exception: " + e.getMessage(), e);
            return getExceptionResponseEntity(e, apiId, null);
        }
    }

    @RequestMapping(value = "/private/assessment/itemset/retire/{id:.+}", method = RequestMethod.DELETE)
    @ResponseBody
    public ResponseEntity<Response> delete(@PathVariable(value = "id") String id,
            @RequestHeader(value = "user-id") String userId) {
    	String taxonomyId = V2_GRAPH_ID;
        String apiId = "assessment_item_set.delete";
        LOGGER.info("Delete | TaxonomyId: " + taxonomyId + " | Id: " + id + " | user-id: " + userId);
        try {
            Response response = assessmentManager.deleteItemSet(id, taxonomyId);
            LOGGER.info("Delete | Response: " + response);
            return getResponseEntity(response, apiId, null);
        } catch (Exception e) {
            LOGGER.error("Delete | Exception: " + e.getMessage(), e);
            return getExceptionResponseEntity(e, apiId, null);
        }
    }

    private Request getSearchRequest(Map<String, Object> requestMap) {
        Request request = getRequest(requestMap);
        Map<String, Object> map = request.getRequest();
        if (null != map && !map.isEmpty()) {
            try {
                ItemSetSearchCriteria criteria = mapper.convertValue(map, ItemSetSearchCriteria.class);
                request.put(AssessmentAPIParams.assessment_search_criteria.name(), criteria);
            } catch (Exception e) {
                throw new MiddlewareException(AssessmentErrorCodes.ERR_ASSESSMENT_INVALID_SEARCH_CRITERIA.name(),
                        "Invalid search criteria.", e);
            }
        } else if (null != map && map.isEmpty()) {
            request.put(AssessmentAPIParams.assessment_search_criteria.name(), new ItemSetSearchCriteria());
        }
        return request;
    }

    private Request getRequestObject(Map<String, Object> requestMap) {
        Request request = getRequest(requestMap);
        Map<String, Object> map = request.getRequest();
        if (null != map && !map.isEmpty()) {
            try {
                Object objItemSet = map.get(AssessmentAPIParams.assessment_item_set.name());
                if (null != objItemSet) {
                    Node itemSetNode = (Node) mapper.convertValue(objItemSet, Node.class);
                    request.put(AssessmentAPIParams.assessment_item_set.name(), itemSetNode);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return request;
    }
}