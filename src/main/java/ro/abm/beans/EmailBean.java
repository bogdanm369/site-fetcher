package ro.abm.beans;

import lombok.Builder;
import lombok.Getter;

import java.util.UUID;

@Getter
@Builder
public class EmailBean {

    private final String id = UUID.randomUUID().toString();

    private String sendTo;
    private String subject;
    private String content;

    @Override
    public boolean equals(Object o) {
        if (o == null || !(o instanceof EmailBean)) {
            return false;
        }
        return this.id.equals(((EmailBean) o).id);
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }
}
