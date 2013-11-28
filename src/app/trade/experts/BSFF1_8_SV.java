package app.trade.experts;

import app.trade.Orden;
import trade.Arithmetic;
import trade.IExpert;
import trade.indicator.base.BollingerBands;


/**
 * BSFF1_8_SV -> Bollinger Sin FIFO 1.8 Salida de Ventas.
 * @author omar
 */
public class BSFF1_8_SV extends Expert implements IExpert{
    //Bandas de bollinger utilizadas.
    
    private BollingerBands b1;
    private BollingerBands b2;
    private BollingerBands b3;
    private BollingerBands bs1;
    private BollingerBands bs2;
    private BollingerBands bs3;
    private BollingerBands bx1;
    private BollingerBands bx2;
    private BollingerBands bx3;
    private Double sl;
    private Double tp; 
    private Double bollSpecial;
    private double bollXDn;
    private double bollXUp;
    private double bollUp;
    private double bollDn;
    private double bollUpS;
    private double bollDnS;
    private double bollDif;
    private Integer velasSalida;
    private Integer limiteCruce;
    public Double horaIni;
    public Double horaFin;
    private Integer contVelas = 0;
    
    /**
     * Inicia las variables.
     */
    @Override
    public void Init() {
        
        b1 = this.iBand(this.extern.getInteger("periodoBoll"));
        b2 = this.iBand(this.extern.getInteger("periodoBoll2"));
        b3 = this.iBand(this.extern.getInteger("periodoBoll3"));
        
        bs1 = this.iBand(this.extern.getInteger("periodoBollSalida"));
        bs2 = this.iBand(this.extern.getInteger("periodoBollSalida2"));
        bs3 = this.iBand(this.extern.getInteger("periodoBollSalida3"));
        
        bx1 = this.iBand(this.extern.getInteger("XBoll"));
        bx2 = this.iBand(this.extern.getInteger("XBoll2"));
        bx3 = this.iBand(this.extern.getInteger("XBoll3"));
        
        bollXDn = this.extern.getDouble("bollXDn") ;
        bollXUp = this.extern.getDouble("bollXUp") ;
        
        sl = Arithmetic.multiplicar(this.extern.getInteger("sl").doubleValue() , this.getPoint());
        tp = Arithmetic.multiplicar(this.extern.getInteger("tp").doubleValue() , this.getPoint());
        bollSpecial = this.extern.getDouble("bollspecial") ;
        velasSalida = this.extern.getInteger("num_velas_salida");
        limiteCruce = this.extern.getInteger("limiteCruce");
        horaIni = this.extern.getDouble("horainicial");
        horaFin = this.extern.getDouble("horafinal");
        
        this.bollUp = this.bollUp();
        this.bollDn = this.bollDn();
        this.bollUpS = this.bollUpS();
        this.bollDnS = this.bollDnS();
        this.bollDif = this.bollDif();
    }
    
    @Override
    public void onTick() {
        
        if(this.isNewCandle()) {
            this.bollUp = this.bollUp();
            this.bollDn = this.bollDn();
            this.bollUpS = this.bollUpS();
            this.bollDnS = this.bollDnS();
            this.bollDif = this.bollDif();
            this.contVelas++;
        }
        
        if(this.isTradeTime() && this.bollDif < this.bollXUp && this.bollDif > this.bollXDn &&
                this.ordersBySymbol() < this.limiteCruce ) {
            
            if(this.getOpenMin() + this.bollSpecial <= this.bollDn) {
                
                double stop = Arithmetic.redondear(this.getAsk() - this.sl);
                double take = Arithmetic.redondear(this.getAsk() + this.tp);
                this.orderSend(1.0, stop, take, '1', this.getAsk());
                contVelas = 0;
                
            } else if(this.getOpenMin() - this.bollSpecial >= this.bollUp) {
                double stop = Arithmetic.redondear(this.getBid() + this.sl);
                double take = Arithmetic.redondear(this.getBid() - this.tp);
                this.orderSend(1.0, stop, take, '2', this.getBid());
                contVelas = 0;
            }
            
        } else if(this.ordersBySymbol() > 0) {
            for (int i = 0; i < this.ordersBySymbol(); i++) {
                Orden o = (Orden)this.ordersTotal(this.getMagic()).get(i);
                
                if(o.getSide() == '2') {
                    if(this.getOpenMin() <= this.bollDnS) {
                        o.close(this.getAsk(), "Cierre por bollinger");
                       
                    } else if(this.contVelas >= this.velasSalida) {
                        o.close(this.getAsk(), "Cierre por velas");
                        
                    }
                }else if(o.getSide() == '1') {
                    if(this.getOpenMin() >= this.bollUpS) {                        
                        o.close(this.getBid(), "Cierre por bollinger");
                        
                    } else if(this.contVelas >= this.velasSalida) {
                        o.close(this.getBid(), "Cierre por velas");                   
                    }
                }
            }
        }
    }

    @Override
    public void onDone() {
        
    }
    /**
     * Define si es tiempo de operar.
     * @return 
     */
    public Boolean isTradeTime(){
        double c = (this.getHora() + (this.getMinutes()*0.01)) + (this.getMinutes() /100);
        return (c < this.horaFin) && (c >= this.horaIni) && this.isReady();
    }
    
    /**
     * Promedio de entrada de ventas.
     * @return 
     */
    public double bollUp() {
        
        return (this.b1.getUpperBand() + this.b2.getUpperBand() + 
                            this.b3.getUpperBand())/3;
    }
    /**
     * Promedio Entrada de compras.
     * @return 
     */
    private double bollDn() {
        return (this.b1.getLowerBand() + this.b2.getLowerBand() +
                            this.b3.getLowerBand())/3;
    }
    
     /**
     * Promedio salida de compras.
     * @return 
     */
    private double bollUpS() {
        return Arithmetic.redondear((this.bs1.getUpperBand() + this.bs2.getUpperBand() +
                            this.bs3.getUpperBand())/3, 7);
    }
    /**
     * Promedio salida de ventas.
     * @return 
     */
    private double bollDnS() {
        return Arithmetic.redondear((this.bs1.getLowerBand() + this.bs2.getLowerBand() +
                            this.bs3.getLowerBand())/3, 7);
    }
    
    private double bollDif() {
        double tempUp = (this.bx1.getUpperBand() + this.bx2.getUpperBand() + this.bx3.getUpperBand())/3;
        double tempDn = (this.bx1.getLowerBand() + this.bx2.getLowerBand() + this.bx3.getLowerBand())/3;
        return Arithmetic.redondear(tempUp - tempDn, 7);
    }
    
   
}
