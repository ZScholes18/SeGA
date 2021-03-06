package org.sega.viewer.controllers;

import static org.springframework.web.bind.annotation.RequestMethod.GET;
import static org.springframework.web.bind.annotation.RequestMethod.POST;

import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.sega.viewer.models.Log;
import org.sega.viewer.models.Process;
import org.sega.viewer.models.ProcessInstance;
import org.sega.viewer.repositories.LogRepository;
import org.sega.viewer.repositories.ProcessInstanceRepository;
import org.sega.viewer.services.JtangEngineService;
import org.sega.viewer.services.ProcessInstanceService;
import org.sega.viewer.services.ProcessService;
import org.sega.viewer.services.support.Node;
import org.sega.viewer.services.support.ProcessJsonResolver;
import org.sega.viewer.utils.Base64Util;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.servlet.ModelAndView;

/**
 * @author Raysmond<i@raysmond.com>.
 */
@Controller
@RequestMapping(value = "processes")
public class ProcessesController {
    @Autowired
    private ProcessService processService;

    @Autowired
    private ProcessInstanceService processInstanceService;

    @Autowired
    private ProcessInstanceRepository processInstanceRepository;

    @Autowired
    private JtangEngineService jtangEngineService;
    @Autowired
    private LogRepository logRepository;
    

    private static final int pageSize = 6;
    private static final Logger logger = LoggerFactory.getLogger(ProcessesController.class);
    @RequestMapping(value = "", method = GET)
    public String processes(Model model,@RequestParam(defaultValue = "0") int page){
    	page = page < 0 ? 0 : page;
    	RequestAttributes ra = RequestContextHolder.getRequestAttributes();  
        HttpServletRequest request = ((ServletRequestAttributes)ra).getRequest();
        String city = (String) request.getSession().getAttribute("city");
    	List<Process> processes = processService.getAllProcessesByCity(city);
        model.addAttribute("processes", processes);
        model.addAttribute("totalProcesses", processes.size());
        model.addAttribute("page",page+1);
        model.addAttribute("totalPages", processes.size() / pageSize);
        return "processes/index";
        
    }

    @RequestMapping(value = "{processId:\\d+}", method = GET)
    public String showProcess(@PathVariable Long processId, Model model){
        Process process = processService.getProcess(processId);

        model.addAttribute("process", process);
        model.addAttribute("instances", processInstanceService.getProcessInstances(process));
        model.addAttribute("processJson", decodeJson(process.getProcessJSON()));
        model.addAttribute("processXML", decodeJson(process.getProcessXML()));
        model.addAttribute("entityJson", decodeJson(process.getEntityJSON()));
        model.addAttribute("bindingResultJson", decodeJson(process.getBindingJson()));
        model.addAttribute("edMappingJson", decodeJson(process.getEDmappingJSON()));
        model.addAttribute("databaseJson", decodeJson(process.getDatabaseJSON()));
        model.addAttribute("ddMappingJson", decodeJson(process.getDDmappingJSON()));
        return "processes/show";
    }

    @RequestMapping(value = "{processId:\\d+}", method = POST)
    public String createProcessInstance(@PathVariable Long processId) throws UnsupportedEncodingException, MalformedURLException {
    	Process process = processService.getProcess(processId);
        ProcessInstance instance = processInstanceService.createProcessInstance(process);

        jtangEngineService.publishProcess(instance);
        
        //记录日志操作
        Log log = new Log();
        Date date=new Date(); 
        SimpleDateFormat df=new SimpleDateFormat("yyyy-MM-dd hh:mm:ss"); 
        log.setDate(df.format(date));
        log.setContent("在"+df.format(date)+" ，配置了："+process.getName()+"流程");
        log.setClassName("ProcessController");
        log.setDescriptions("流程配置");
        log.setType("33");
        log.setOperationType("流程配置");
        logRepository.save(log);
        return "redirect:instances/" + instance.getId();
    }

    @RequestMapping(value = "instances/{instanceId:\\d+}", method = GET)
    public String showProcessInstance(@PathVariable Long instanceId, Model model) throws UnsupportedEncodingException {
    	ProcessInstance processInstance = processInstanceRepository.findOne(instanceId);
        model.addAttribute("instance", processInstance);
        model.addAttribute("processJSON", Base64Util.decode(processInstance.getProcess().getProcessJSON()));

        ProcessJsonResolver processJsonResolver = new ProcessJsonResolver(processInstance.getProcess().getProcessJSON());
        Node node = processJsonResolver.findNode(processInstance.getNextTask());
        String page = "";  
        if(node != null){
        	model.addAttribute("nextTask", node);
            if(node.getType().equals("sega.Task")){
                page = "redirect:/processes/instances/"+processInstance.getId()+"/task/"+node.getId();
            }else if(node.getType().equals("sega.Service")){
                page = "instances/show_service";
            }
        }else{
        	page = "";
        }
        
        return page;
    }

    @RequestMapping(value = "{processId:\\d+}/templates/{taskId}", method = GET)
    public ModelAndView getTaskTemplate(@PathVariable Long processId, @PathVariable String taskId){
    	ModelAndView template = new ModelAndView("fragments/humantask/"+processId+"/"+taskId);
        return template;
    }

    private String decodeJson(String json){
        String result = null;
        try {
            result = Base64Util.decode(json);
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        return result;
    }
}
