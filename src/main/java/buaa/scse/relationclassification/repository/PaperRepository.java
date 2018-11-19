package buaa.scse.relationclassification.repository;

import buaa.scse.relationclassification.entity.Paper;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PaperRepository extends JpaRepository<Paper, Integer> {
    Paper findByFieldTypeAndContent(int fieldType, String content);
    List<Paper> findAllByFieldTypeAndContentContains(int relationType, String content);
}
