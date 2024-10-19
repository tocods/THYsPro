package api.util;

import api.info.FaultInfo;
import api.info.HostInfo;
import api.info.JobInfo;
import cloudsim.Log;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import faulttolerant.faultGenerator.FaultGenerator;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

public class JsonUtil {

    public List<HostInfo> parseHosts(String path) {
        String s = JsonUtil.readJsonFile(path);
        JSONArray objList = JSON.parseArray(s);
        List<HostInfo> hostInfoList = new ArrayList<>();
        assert objList != null;
        for(Object obj: objList) {
            HostInfo hostInfo = JSON.parseObject(obj.toString(), HostInfo.class);
            hostInfoList.add(hostInfo);
            hostInfo.print();
        }
        return hostInfoList;
    }

    public List<JobInfo> parseJobs(String path) {
        String s = JsonUtil.readJsonFile(path);
        JSONArray objList = JSON.parseArray(s);
        List<JobInfo> jobInfoList = new ArrayList<>();
        assert objList != null;
        for(Object obj: objList) {
            JobInfo jobInfo = JSON.parseObject(obj.toString(), JobInfo.class);
            jobInfoList.add(jobInfo);
            jobInfo.print();
        }
        return jobInfoList;
    }

    public List<FaultInfo> parseFaults(String path) {
        String s = JsonUtil.readJsonFile(path);
        JSONArray objList = JSON.parseArray(s);
        List<FaultInfo> faultInfoList = new ArrayList<>();
        assert objList != null;
        for(Object obj: objList) {
            FaultInfo faultInfo = JSON.parseObject(obj.toString(), FaultInfo.class);
            faultInfoList.add(faultInfo);
            faultInfo.print();
        }
        return faultInfoList;
    }

    /**
     * 读取json文件，返回json串
     * @param fileName
     * @return
     */
    public static String readJsonFile(String fileName) {
        String jsonStr = "";
        try {
            File jsonFile = new File(fileName);
            FileReader fileReader = new FileReader(jsonFile);

            Reader reader = new InputStreamReader(new FileInputStream(jsonFile),"utf-8");
            int ch = 0;
            StringBuffer sb = new StringBuffer();
            while ((ch = reader.read()) != -1) {
                sb.append((char) ch);
            }
            fileReader.close();
            reader.close();
            jsonStr = sb.toString();
            return jsonStr;
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

}