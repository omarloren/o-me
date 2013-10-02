package app.trade;

import app.metrics.MetricsController;
import app.trade.experts.BSFF1_8_SV;
import com.mongodb.DBObject;
import java.util.ArrayList;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import trade.Arithmetic;
import util.Settings;
import util.Date;
import util.Candle;

/**
 *
 * @author omar
 */
public class Gear {
    
    private Broker broker;
    private BSFF1_8_SV expert;
    //private Prueba expert;
    private Settings settings;    
    private String symbol;
    private Integer periodo;
    private Candle candle;
    private int lastDay = 0;
    private int lastMonth = 1;
    
    public Gear(Settings settings, Map<String, Object> it) {
        this.settings = settings;
        this.expert = new BSFF1_8_SV();
        //this.expert = new Prueba();
        this.symbol = this.settings.getSymbol();
        this.periodo = this.settings.getPeriod();
        this.candle = new Candle(this.periodo);
        this.candle.setStrict(false);
        this.broker = new Broker(this.settings.getInitialWon());        
        this.broker.setSpread(this.settings.getSpread());
        this.expert.build(this.periodo).__construct(this.broker,Integer.valueOf(this.settings.getFrom()), this.symbol,this.settings.getPoint(), this.settings.getMagic());   
        Extern extern = new Extern(it);
        this.expert.setExtern(extern);
        this.expert.Init();
    }
    
    public void tick(DBObject t) {
        try {
            Date.setTime(String.valueOf(t.get("DTYYYYMMDD")), String.valueOf(t.get("TIME")));
            ArrayList<Double> arr = this.evaluate(t);
            //Thread.sleep(10);
            Double open = arr.get(0);
            if(Date.getMonth() != this.lastMonth) {
                this.lastMonth = Date.getMonth();
                MetricsController.refresh(Date.getDate(),this.broker.getBalance());
            }
            this.expert.setOpenMin(open);
            if (this.candle.isNew(Date.getMinutes()) /*|| this.isABeatifulDay(Date.getDay())*/){
                this.broker.setOpenMin(open);
            }
            
            for (int i = 0; i < arr.size(); i++) {
                Double bid =arr.get(i);
                Double ask = Arithmetic.sumar(bid, this.settings.getSpread());
                this.broker.ticker(bid);
                this.expert.setBid(bid);  
                this.expert.setAsk(ask);
                this.expert.onTick();
            }
        } catch (Exception ex) {
            Logger.getLogger(Gear.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    
    /**
     * Evalua la vela de minuto y filtra los precios en el orden que deberán ser
     * procesados.
     * @param e
     * @return Precios filtrados.
     */
    private ArrayList<Double> evaluate(DBObject e) {
        ArrayList<Double> r = new ArrayList();
        Double o = (Double)e.get("OPEN");
        Double h = (Double)e.get("HIGH");
        Double l = (Double)e.get("LOW");
        Double c = (Double)e.get("CLOSE");
        Double abs = Arithmetic.redondear(Math.abs(o-c)) * 10000;
        
        /**
         * Por alguna razón MT da como segundo tick el LOW si es que el HIGH y el
         * CLOSE son iguales, sino el HIGH es primero. Además Genera ticks 
         */
        if(Arithmetic.equals(h, c) && !Arithmetic.equals(h, l) && !Arithmetic.equals(o, l) &&
                !Arithmetic.equals(o, h)){
            r.add(o);
            r.add(l);
            r.add(h);
        } else if(Arithmetic.equals(o, h) && Arithmetic.equals(l, c) && (abs > 0)){
            double d = Arithmetic.redondearUp(((o+l)/2), 4);
            r.add(o);
            r.add(d);
            r.add(Arithmetic.redondearUp(((d+l)/2), 4));
        }else if(Arithmetic.equals(o, l) && Arithmetic.equals(h, c) && (abs > 0)){
            if(!(abs >= 2)){
               r.add(o);
               r.add(Arithmetic.redondear(o+ 0.0001)); 
            } else if(abs == 3){
                r.add(o);
                r.add(Arithmetic.redondear(o+ 0.0001)); 
                
            } else {
                Double rel = abs / 3;
                r.add(o);
                r.add(Arithmetic.redondear(o+(rel*0.0001)));
            }
        } else {
            r.add(o);
            r.add(h);
            r.add(l);
        }
        r.add(c);
        Double base = r.get(0);
        for (int i = 1; i < r.size(); i++) {   
            if(Double.compare(base , r.get(i)) == 0){
                r.remove(i);
                i--;
            }else {
                base = r.get(i);
            }
        }
        assert r.size() <= 4;
        /*if(Date.getDate().equals("20081218")){
            System.out.println(Date.dateToString() + " " +r + " >>> O:" +  e.get("OPEN") + " H:" +e.get("HIGH") + " L:" + e.get("LOW") + " C:" +  e.get("CLOSE")); 
        }*/
        
        //System.out.println(Date.dateToString() + " " +r + " >>> O:" +  e.get("OPEN") + " H:" +e.get("HIGH") + " L:" + e.get("LOW") + " C:" +  e.get("CLOSE")); 
        return r;
    }
    
    public Broker getBroker() {
        return this.broker;
    }
    
    public void flush() {
         MetricsController.flushMetrics(this.broker.getBalance());
         this.broker = new Broker(this.settings.getInitialWon()); 
    }
    /**
     * Devuelve si un día es hermoso, o nuevo, o algo. 
     * @param day
     * @return 
     */
    private Boolean isABeatifulDay(int day){
        if(this.lastDay != day){
            this.lastDay = day;
            //Candler vuelve a s estado inicial.
            this.expert.getCandle().reset();
            return true;
        }else{
            return false;
        }
    }
}