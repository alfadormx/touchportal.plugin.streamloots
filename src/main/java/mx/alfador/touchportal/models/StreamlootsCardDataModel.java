package mx.alfador.touchportal.models;

import java.util.List;

public class StreamlootsCardDataModel {

    public String cardName;
    public String description;
    public List<StreamlootsDataFieldModel> fields;

    public String getMessage() {
        StreamlootsDataFieldModel field = this.fields.stream().filter(f -> f.name.equals("message")).findFirst().orElse(null);
        return (field != null) ? field.value : "";
    }

    public String getLongMessage() {
        StreamlootsDataFieldModel field = this.fields.stream().filter(f -> f.name.equals("longMessage")).findFirst().orElse(null);
        return (field != null) ? field.value : "";
    }

    public String getUsername() {
        StreamlootsDataFieldModel field = this.fields.stream().filter(f -> f.name.equals("username")).findFirst().orElse(null);
        return (field != null) ? field.value : "";
    }
}