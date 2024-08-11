package com.dg.ServerRebornFarmguard.entity;


import jakarta.persistence.*;

@Entity
@Table(name = "movements")
public class MovementsEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long idmovements;
    private Long idUser;
    private String nameUser;
    private String positionUser;
    private String departmentUser;
    private String comingTime;
    private String exitTime;
    private String timeAtPlace;
    private String date;
    private String place;
    private String openClose;
    private String openCloseTime;
    private String botMove;

    public MovementsEntity(Long idUser, String nameUser, String positionUser, String departmentUser, String comingTime, String exitTime, String timeAtPlace, String date, String place, String openClose, String openCloseTime, String botMove) {
        this.idUser = idUser;
        this.nameUser = nameUser;
        this.positionUser = positionUser;
        this.departmentUser = departmentUser;
        this.comingTime = comingTime;
        this.exitTime = exitTime;
        this.timeAtPlace = timeAtPlace;
        this.date = date;
        this.place = place;
        this.openClose = openClose;
        this.openCloseTime = openCloseTime;
        this.botMove = botMove;
    }
    public MovementsEntity (){

    }

    public Long getIdmovements() {
        return idmovements;
    }

    public void setIdmovements(Long idmovements) {
        this.idmovements = idmovements;
    }

    public Long getIdUser() {
        return idUser;
    }

    public void setIdUser(Long idUser) {
        this.idUser = idUser;
    }

    public String getNameUser() {
        return nameUser;
    }

    public void setNameUser(String nameUser) {
        this.nameUser = nameUser;
    }

    public String getPositionUser() {
        return positionUser;
    }

    public void setPositionUser(String positionUser) {
        this.positionUser = positionUser;
    }

    public String getDepartmentUser() {
        return departmentUser;
    }

    public void setDepartmentUser(String departmentUser) {
        this.departmentUser = departmentUser;
    }

    public String getComingTime() {
        return comingTime;
    }

    public void setComingTime(String comingTime) {
        this.comingTime = comingTime;
    }

    public String getExitTime() {
        return exitTime;
    }

    public void setExitTime(String exitTime) {
        this.exitTime = exitTime;
    }

    public String getTimeAtPlace() {
        return timeAtPlace;
    }

    public void setTimeAtPlace(String timeAtPlace) {
        this.timeAtPlace = timeAtPlace;
    }

    public String getDate() {
        return date;
    }

    public void setDate(String date) {
        this.date = date;
    }

    public String getPlace() {
        return place;
    }

    public void setPlace(String place) {
        this.place = place;
    }

    public String getOpenClose() {
        return openClose;
    }

    public void setOpenClose(String openClose) {
        this.openClose = openClose;
    }

    public String getOpenCloseTime() {
        return openCloseTime;
    }

    public void setOpenCloseTime(String openCloseTime) {
        this.openCloseTime = openCloseTime;
    }

    public String getBotMove() {
        return botMove;
    }

    public void setBotMove(String botMove) {
        this.botMove = botMove;
    }
}
