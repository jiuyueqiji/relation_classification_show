package buaa.scse.relationclassification;

import buaa.scse.relationclassification.util.MMSegment;

import java.nio.charset.Charset;
import java.nio.file.Paths;

import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import com.huaban.analysis.jieba.JiebaSegmenter;
import com.huaban.analysis.jieba.WordDictionary;

@SpringBootApplication
public class RelationclassificationApplication implements CommandLineRunner {

    public static MMSegment mmSegment = new MMSegment();
    public static JiebaSegmenter segmenter = new JiebaSegmenter();

    @Override
    public void run(String... args) {
    	WordDictionary wordDictionary = WordDictionary.getInstance();
		wordDictionary.loadUserDict(Paths.get("D:/Workspaces/Eclipse/dictionary.dict"), Charset.defaultCharset());
		mmSegment.addDictionary();
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
