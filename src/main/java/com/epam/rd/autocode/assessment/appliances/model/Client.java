package com.epam.rd.autocode.assessment.appliances.model;

public class Client extends User {

    private String card;

    public Client() {}

    public Client(Long id, String name, String email, String password, String card) {
        super(id ,name, email, password);
        this.card = card;
    }

    public String getCard() {
        return card;
    }

    public void setCard(String card) {
        this.card = card;
    }
}
