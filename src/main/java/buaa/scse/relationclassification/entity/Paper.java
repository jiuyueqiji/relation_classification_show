package buaa.scse.relationclassification.entity;

import lombok.Data;

import javax.persistence.*;

@Data
@Entity(name = "paper")
public class Paper {
    @Id
    @GeneratedValue(strategy= GenerationType.AUTO)
    private int id;
    private int relationType;
    private int fieldType;
    private int entityOnePosition;
    private int entityTwoPosition;
    @Column(columnDefinition = "text")
    private String content;
}
