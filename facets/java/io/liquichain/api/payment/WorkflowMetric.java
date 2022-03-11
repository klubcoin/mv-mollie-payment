package io.liquichain.api.payment;

import org.meveo.service.script.Script;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.meveo.admin.exception.BusinessException;
import org.meveo.model.customEntities.MoOrder;
import org.meveo.model.storage.Repository;
import org.meveo.service.storage.RepositoryService;
import org.meveo.api.persistence.CrossStorageApi;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class WorkflowMetric extends Script {
    
    
    private static final Logger log = LoggerFactory.getLogger(WorkflowMetric.class);
    
    private CrossStorageApi crossStorageApi = getCDIBean(CrossStorageApi.class);
    private RepositoryService repositoryService = getCDIBean(RepositoryService.class);
    private Repository defaultRepo = repositoryService.findDefaultRepository();

    private String result;


    public String getResult() {
        return result;
    }

    private static String serializeOrderKey(MoOrder order){
        return String.format("status=\"%s\",user_group=\"%s\",user=\"%s\"}",order.getStatus(),order.getGroup(),order.getAssignedTo());
    }

	@Override
	public void execute(Map<String, Object> parameters) throws BusinessException {
        result = "# HELP workflow_status_total The total number of workflow instances.\n"
        +"# TYPE workflow_status_total counter\n";
        //FIXME: do not agregate in memory, use a SQL query dummy, that is the entire point of storing the entities in SQL
        List<MoOrder> orders = new ArrayList<>();
        try{
            orders = crossStorageApi.find(defaultRepo, MoOrder.class).getResults();
        } catch(Exception e){
            e.printStackTrace();
        }
        long time = System.currentTimeMillis();
        String lines ="";
        Map<String,Map<String,Map<String,Long>>> aggregates = new HashMap<>();
        orders.stream()
            .collect(Collectors.groupingBy(order -> serializeOrderKey(order), Collectors.counting()))
            .forEach((id,count)->{result+="workflow_status_total{process=\"order\","+id+" "+count+" "+time+"\n";});
    }
}