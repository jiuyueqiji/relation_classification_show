package buaa.scse.relationclassification;

import buaa.scse.relationclassification.controller.HelloController;
import buaa.scse.relationclassification.util.MMSegment;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class RelationclassificationApplication implements CommandLineRunner {

    @Autowired
    private HelloController helloController;

    public static MMSegment mmSegment = new MMSegment();

    @Override
    public void run(String... args) {
        mmSegment.getDictionary();
        /*try {
            helloController.savePapers("wuli");
            helloController.savePapers("huaxue");
        } catch (IOException e) {
            e.printStackTrace();
        }
        System.out.println("hello world");*/
    }

    public static void main(String[] args) {
        SpringApplication.run(RelationclassificationApplication.class, args);
    }
}
