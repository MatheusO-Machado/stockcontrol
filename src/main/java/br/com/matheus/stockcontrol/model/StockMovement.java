package br.com.matheus.stockcontrol.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class StockMovement {
    private Long id;
    private MovementType type;
    private LocalDateTime dateTime;
    private BigDecimal total;
    private String observation;

    private List<StockMovementItem> items = new ArrayList<>();

    public StockMovement() {}

    public StockMovement(Long id, MovementType type, LocalDateTime dateTime, BigDecimal total, String observation) {
        this.id = id;
        this.type = type;
        this.dateTime = dateTime;
        this.total = total;
        this.observation = observation;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public MovementType getType() { return type; }
    public void setType(MovementType type) { this.type = type; }

    public LocalDateTime getDateTime() { return dateTime; }
    public void setDateTime(LocalDateTime dateTime) { this.dateTime = dateTime; }

    public BigDecimal getTotal() { return total; }
    public void setTotal(BigDecimal total) { this.total = total; }

    public String getObservation() { return observation; }
    public void setObservation(String observation) { this.observation = observation; }

    public List<StockMovementItem> getItems() { return items; }
    public void setItems(List<StockMovementItem> items) { this.items = items; }
}