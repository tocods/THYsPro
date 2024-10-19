package api.info;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class JobRunningInfo {
    public String host = "";

    public String start = "";

    public String duration = "";

    public String end = "";

    public String status = "";

    public List<TaskRunInfo> runInfos = new ArrayList<>();
}
