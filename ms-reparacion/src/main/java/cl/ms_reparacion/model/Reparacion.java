package cl.ms_reparacion.model;

import jakarta.persistence.*;

@Entity
@Table(name = "reparaciones")
public class Reparacion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "rut_cliente", nullable = false)
    private String rutCliente;

    @Column(nullable = false)
    private String modelo;

    @Column(nullable = false)
    private String descripcion;

    @Column(nullable = false)
    private String estado;

    public Reparacion() {}

    public Long getId() { return id; }
    public String getRutCliente() { return rutCliente; }
    public void setRutCliente(String rutCliente) { this.rutCliente = rutCliente; }
    public String getModelo() { return modelo; }
    public void setModelo(String modelo) { this.modelo = modelo; }
    public String getDescripcion() { return descripcion; }
    public void setDescripcion(String descripcion) { this.descripcion = descripcion; }
    public String getEstado() { return estado; }
    public void setEstado(String estado) { this.estado = estado; }
}