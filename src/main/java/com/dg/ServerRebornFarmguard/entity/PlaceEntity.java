package com.dg.ServerRebornFarmguard.entity;


import jakarta.persistence.*;

@Entity
@Table(name = "places")
public class PlaceEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long idplace;
    private String namePlace;
    private String macPlace;
    private String itsHub;

    public String getItsHub() {
        return itsHub;
    }

    public void setItsHub(String itsHub) {
        this.itsHub = itsHub;
    }

    public Long getIdplace() {
        return idplace;
    }

    public void setIdplace(Long idplace) {
        this.idplace = idplace;
    }

    public String getNamePlace() {
        return namePlace;
    }

    public void setNamePlace(String namePlace) {
        this.namePlace = namePlace;
    }

    public String getMacPlace() {
        return macPlace;
    }

    public void setMacPlace(String macPlace) {
        this.macPlace = macPlace;
    }
}
