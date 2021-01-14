package WorthServer;

import com.fasterxml.jackson.annotation.JsonIgnore;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.StringTokenizer;

public class Card {
        // Informazione della card (nome, descrizione) e Lista di movimenti tra le liste di stato nel progetto
        private String name;
        private String description;
        private List<String> updates;

        //Costruttori
        public Card(String name, String description){
            this.name = name;
            this.description = description;
            this.updates = new ArrayList<>();
            this.updates.add(ProjectImpl.cardList.TODO + " " +  (new Date()).toInstant().getEpochSecond());
        }
        // Costruttore per jackson
        public Card() {
            this.name = null;
            this.description = null;
            this.updates = new ArrayList<>();
        }

        //Getter e Setter per gli attributi
        public String getName() {
            return name;
        }
        public void setName(String name) { this.name = name;}
        public String getDescription() {
            return description;
        }
        public void setDescription(String description) { this.description = description;}
        public List<String> getUpdates(){ return updates; }
        public void setUpdates(List<String> updates){ this.updates = updates;}

        // Aggiornare lo stato della card, con la data corrente e la nuova lista in cui si trova
        public void update(ProjectImpl.cardList st){
            this.updates.add(st + " " + (new Date()).toInstant().getEpochSecond());
        }

        // Get current card state ( last state )
        @JsonIgnore
        public ProjectImpl.cardList getState(){
            StringTokenizer tokenizer = new StringTokenizer(this.updates.get(updates.size()-1));
            String l = tokenizer.nextToken();
            return ProjectImpl.toStaticCardlist(l);
        }


        // Static Get date from a string s ( s must be like this = "date cardlist";
        @JsonIgnore
        public static Date getDate(String s){
            StringTokenizer tokenizer = new StringTokenizer(s);
            tokenizer.nextToken();
            String l = tokenizer.nextToken();
            return new Date(Long.parseLong(l) * 1000);
        }

        // Static Get state from a string s ( s must be like this = "date cardlist";
        @JsonIgnore
        public static ProjectImpl.cardList getState(String s){
            StringTokenizer tokenizer = new StringTokenizer(s);
            String l = tokenizer.nextToken();
            return ProjectImpl.toStaticCardlist(l);
        }
}
