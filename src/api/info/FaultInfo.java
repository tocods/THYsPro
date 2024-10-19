package api.info;

import cloudsim.Log;
import de.vandermeer.asciitable.AsciiTable;
import de.vandermeer.skb.interfaces.transformers.textformat.TextAlignment;
import faulttolerant.faultGenerator.*;
import faulttolerant.faultInject.FaultInjector;

public class FaultInfo {
    public String aim;

    public String mttfType;

    public String mttfScale;

    public String mttfShape;

    public String mttrType;

    public String mttrScale;

    public String mttrShape;

    public String print() {
        AsciiTable at = new AsciiTable();
        at.addRule();
        at.addRow("aim", "MTTF", "scale", "shape", "MTTR", "scale", "shape");
        at.addRule();
        at.addRow(aim, mttfType, mttfScale, mttfShape, mttrType, mttrScale, mttrShape);
        at.addRule();
        at.setTextAlignment(TextAlignment.CENTER);
        Log.printLine(at.render());
        return at.render();
    }

    public FaultGenerator tran2Generator() {
        FaultGenerator ret = null;
        switch (mttfType) {
            case "Normal":
                ret = new NormalGenerator();
                break;
            case "LogNormal":
                ret = new LogNormalGenerator();
                break;
            case "Weibull":
                ret = new WeibullGenerator();
                break;
            case "Gamma":
                ret = new GammaGenerator();
                break;
            default:
                ret = new NormalGenerator();
                break;
        }
        ret.initSamples(Double.parseDouble(mttfScale), Double.parseDouble(mttfShape));
        ret.initRepair(Double.parseDouble(mttrScale), Double.parseDouble(mttrShape));
        return ret;
    }
}
