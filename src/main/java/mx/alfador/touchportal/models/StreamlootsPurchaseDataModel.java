package mx.alfador.touchportal.models;

import java.util.List;

public class StreamlootsPurchaseDataModel {

    public List<StreamlootsDataFieldModel> fields;

    public String getGiftee() {
        StreamlootsDataFieldModel field = this.fields.stream().filter(f -> f.name.equals("giftee")).findFirst().orElse(null);
        return (field != null) ? field.value : "";
    }

    public int getQuantity() {
        StreamlootsDataFieldModel field = this.fields.stream().filter(f -> f.name.equals("quantity")).findFirst().orElse(null);
        return (field != null) ? Integer.parseInt(field.value) : 0;
    }

    public String getUsername() {
        StreamlootsDataFieldModel field = this.fields.stream().filter(f -> f.name.equals("username")).findFirst().orElse(null);
        return (field != null) ? field.value : "";
    }
}
