package app.metrics;

import app.metrics.base.Metric;
import app.metrics.base.Pain;
import app.metrics.base.StdDev;
import java.util.ArrayList;
import trade.Arithmetic;

/**
 * Controlador de métricos hedge funds.
 *
 * @author omar
 */
public class MetricsController {

    /**
     * Guarda una serie un conjunto de métricos.
     */
    private ArrayList<Metric> metricsPool = new ArrayList();
    private Double lastValue;

    /**
     * Añade un nuevo Pain metric.
     *
     * @param id
     * @param initialAccount
     * @param from
     * @param to
     * @return
     */
    public Pain newPain(String id, Integer initialAccount, String from, String to) {
        Pain p = new Pain(id, initialAccount, from, to);
        metricsPool.add(p);
        lastValue = new Double(initialAccount);
        return p;
    }


    /**
     * Añade un nuevo controlador de Desviaciones estadard
     *
     * @param id
     * @param from
     * @param to
     * @return
     */
    public StdDev newStdDev(String id, String from, String to) {
        StdDev stdDev = new StdDev(id, from, to);
        metricsPool.add(stdDev);
        return stdDev;
    }

    /**
     * Actualiza los métricos.
     *
     * @param date
     * @param val
     */
    public void refresh(String date, Double val) {
        for (int i = 0; i < metricsPool.size(); i++) {
            Metric m = metricsPool.get(i);
            if (m.isActive(date) && m.canRefresh()) {
                //si es la primera vez que insertamos un precio.
                if (m.isNew()) {
                    m.setLastValue(lastValue);
                }
                m.feed(val);
            }
        }
        lastValue = val;
    }

    /**
     * Devuelve el total de métricos.
     *
     * @return
     */
    public ArrayList<Metric> getMetrics() {
        return metricsPool;
    }

    /**
     * Calcula el IR tomando en cuenta la relación entre las ganancias de un
     * periodo corto con uno largo.
     *
     * @return
     */
    public Double getIR() {
        Double _long = this.getPain("LONG").getMonthlyAvg();
        Double _short = this.getPain("SHORT").getMonthlyAvg();
       
        //IR 
        return Arithmetic.redondear((_short / _long), 4);
    }
   
    /**
     * Devulve un determinado pain index.
     *
     * @param id SHOR/LONG
     * @return
     */
    public Pain getPain(String id) {
        Pain p = null;
        for (int i = 0; i < metricsPool.size(); i++) {
            if (metricsPool.get(i).getClass() == Pain.class) {
                if (metricsPool.get(i).getId().equals(id)) {
                    p = (Pain) metricsPool.get(i);
                }
            }
        }
        return p;
    }

    /**
     * Devuelve un determinado STD.
     *
     * @param id
     * @return
     */
    public StdDev getStd(String id) {
        StdDev s = null;
        for (int i = 0; i < metricsPool.size(); i++) {
            if (metricsPool.get(i).getClass() == StdDev.class) {
                if (metricsPool.get(i).getId().equals(id)) {
                    s = (StdDev) metricsPool.get(i);
                    
                }
            }
        }
        return s;
    }
}
