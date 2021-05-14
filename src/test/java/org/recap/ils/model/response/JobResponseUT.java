package org.recap.ils.model.response;

import org.junit.Test;
import org.recap.BaseTestCaseUT;
import org.recap.ils.protocol.rest.model.DebugInfo;
import org.recap.ils.protocol.rest.model.JobData;
import org.recap.ils.protocol.rest.model.response.JobResponse;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertNotNull;

public class JobResponseUT extends BaseTestCaseUT {


    @Test
    public void testJobResponse() {
        JobResponse jobResponse = new JobResponse();
        JobData jobdata = new JobData();
        List<DebugInfo> debuginfolist = new ArrayList<>();
        DebugInfo debugInfo = new DebugInfo();
        debuginfolist.add(debugInfo);
        jobResponse.setCount(1);
        jobResponse.setStatusCode(2);
        jobResponse.setDebugInfo(debuginfolist);
        jobResponse.setData(jobdata);
        jobResponse.getCount();
        jobResponse.getStatusCode();
        jobResponse.getDebugInfo();
        assertNotNull(jobResponse.getCount());
        assertNotNull(jobResponse.getStatusCode());
        assertNotNull(jobResponse.getDebugInfo());
    }
}
