/*
 * Title:        CloudSim Toolkit
 * Description:  CloudSim (Cloud Simulation) Toolkit for Modeling and Simulation of Clouds
 * Licence:      GPL - http://www.gnu.org/copyleft/gpl.html
 *
 * Copyright (c) 2009-2012, The University of Melbourne, Australia
 */

package cloudsim.power.models;

/**
 * The power model of an IBM server x3250 (1 x [Xeon X3470 2933 MHz, 4 cores], 8GB).<br/>
 * <a href="http://www.spec.org/power_ssj2008/results/res2009q4/power_ssj2008-20091104-00213.html">
 * http://www.spec.org/power_ssj2008/results/res2009q4/power_ssj2008-20091104-00213.html</a>
 * 
 * <br/>If you are using any algorithms, policies or workload included in the power package please cite
 * the following paper:<br/>
 * 
 * <ul>
 * <li><a href="http://dx.doi.org/10.1002/cpe.1867">Anton Beloglazov, and Rajkumar Buyya, "Optimal Online Deterministic Algorithms and Adaptive
 * Heuristics for Energy and Performance Efficient Dynamic Consolidation of Virtual Machines in
 * Cloud Data Centers", Concurrency and Computation: Practice and Experience (CCPE), Volume 24,
 * Issue 13, Pages: 1397-1420, John Wiley & Sons, Ltd, New York, USA, 2012</a>
 * </ul>
 * 
 * @author Anton Beloglazov
 * @since CloudSim Toolkit 3.0
 */
public class PowerModelSpecPowerIbmX3250XeonX3470 extends PowerModelSpecPower {
	/** 
         * The power consumption according to the utilization percentage. 
         * @see #getPowerData(int) 
         */
	private final double[] power = { 41.6, 46.7, 52.3, 57.9, 65.4, 73, 80.7, 89.5, 99.6, 105, 113 };

	@Override
	protected double getPowerData(int index) {
		return power[index];
	}

}
