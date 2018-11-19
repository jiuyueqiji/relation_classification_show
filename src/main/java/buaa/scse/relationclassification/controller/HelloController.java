package buaa.scse.relationclassification.controller;

import buaa.scse.relationclassification.RelationclassificationApplication;
import buaa.scse.relationclassification.entity.Paper;
import buaa.scse.relationclassification.repository.PaperRepository;
import buaa.scse.relationclassification.util.MMSegment;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.FileSystemResourceLoader;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.io.*;
import java.util.*;

@Controller
public class HelloController {
    int length = 3;
    HashMap<String, Integer> mapping = new HashMap() {
        {
            put("wuli", 1);
            put("huaxue", 2);
        }
    };

    ArrayList<String> symbol = new ArrayList() {
        {
            add(",");
            add(".");
            add(";");
            add("，");
            add("。");
            add("；");
        }
    };

    @Autowired
    private PaperRepository paperRepository;

    @RequestMapping(value = "/search")
    public String search(@RequestParam(value = "field", defaultValue = "wuli") String field,
                         @RequestParam(value = "keyword", defaultValue = "") String keyword,
                         Model model) {
        List<Paper> papers = paperRepository.findAllByFieldTypeAndContentContains(mapping.get(field), keyword);
        for (Paper paper : papers) {
            String content = paper.getContent();
            String[] tokens = content.split(" ");
            StringBuilder stringBuilder = new StringBuilder();
            for (int index = 0; index < tokens.length; index++) {
                if (tokens[index].equals(keyword)) {
                    stringBuilder.append("<font color='red'><strong>");
                    stringBuilder.append(tokens[index]);
                    stringBuilder.append("</strong></font>");
                } else if (index == paper.getEntityOnePosition() || index == paper.getEntityTwoPosition()) {
                    stringBuilder.append("<font color='red'><strong>");
                    stringBuilder.append(tokens[index]);
                    stringBuilder.append("</strong></font>");
                } else if (symbol.contains(tokens[index]) && index==tokens.length-1) {
                    continue;
                } else {
                    stringBuilder.append(tokens[index]);
                }
            }
            paper.setContent(stringBuilder.toString());
        }
        if (papers == null || papers.size() == 0) {
            List<String> keywords = RelationclassificationApplication.mmSegment.test(keyword);
            for (String k : keywords) {
                papers.addAll(paperRepository.findAllByFieldTypeAndContentContains(mapping.get(field), k));
            }
            for (Paper paper : papers) {
                String content = paper.getContent();
                String[] tokens = content.split(" ");
                StringBuilder stringBuilder = new StringBuilder();
                for (int index = 0; index < tokens.length; index++) {
                    if (symbol.contains(tokens[index]) && index==tokens.length-1) {
                        continue;
                    }
                    stringBuilder.append(tokens[index]);
                }
                if (stringBuilder.indexOf(keyword) != -1) {
                    stringBuilder.insert(stringBuilder.indexOf(keyword), "<font color='red'><strong>");
                    stringBuilder.insert(stringBuilder.indexOf(keyword)+keyword.length(), "</strong></font>");
                }
                /*for (int index = 0; index < tokens.length; index++) {
                    if (keywords.contains(tokens[index])) {
                        stringBuilder = appendStringBuilder(stringBuilder, tokens[index]);
                    } else if (index == paper.getEntityOnePosition() || index == paper.getEntityTwoPosition()) {
                        stringBuilder = appendStringBuilder(stringBuilder, tokens[index]);
                    } else if (symbol.contains(tokens[index]) && index==tokens.length-1) {
                        continue;
                    } else {
                        stringBuilder.append(tokens[index]);
                    }
                }*/
                paper.setContent(stringBuilder.toString());
            }
        }
        papers = deleteDuplicate(papers);
        model.addAttribute("papers", papers);
        model.addAttribute("field", field);
        model.addAttribute("keyword", keyword);
        return "index";
    }

    private List<Paper> deleteDuplicate(List<Paper> papers) {
        LinkedHashSet<Paper> set = new LinkedHashSet<Paper>(papers.size());
        set.addAll(papers);
        papers.clear();
        papers.addAll(set);
        return papers;
    }
    private StringBuilder appendStringBuilder(StringBuilder stringBuilder, String token) {
        if (stringBuilder.toString().endsWith("</strong></font>")) {
            String temp = stringBuilder.substring(0, stringBuilder.length() - 5);
            stringBuilder = new StringBuilder(temp);
            stringBuilder.append(token);
            stringBuilder.append("</strong></font>");
        } else {
            stringBuilder.append("<font color='red'><strong>");
            stringBuilder.append(token);
            stringBuilder.append("</strong></font>");
        }
        return stringBuilder;
    }

    public void savePapers(String filePrefix) throws IOException {
        FileSystemResourceLoader fileSystemResourceLoader = new FileSystemResourceLoader();
        Resource resource  = fileSystemResourceLoader.getResource("classpath:/static/"+filePrefix+"relation_describe.txt");

        if (resource.isReadable()) {
            InputStream is = resource.getInputStream();
            BufferedReader br = new BufferedReader(new InputStreamReader(is));

            String line;
            Paper paper;
            int fieldType = mapping.get(filePrefix);

            while ((line = br.readLine()) != null) {
                String[] temp = line.split("\t");
                paper = paperRepository.findByFieldTypeAndContent(fieldType, temp[5]);
                if (paper != null) {
                    continue;
                }
                paper = new Paper();
                paper.setFieldType(fieldType);
                paper.setRelationType(Integer.parseInt(temp[0]));
                paper.setEntityOnePosition(Integer.parseInt(temp[1]));
                paper.setEntityTwoPosition(Integer.parseInt(temp[3]));
                paper.setContent(temp[5]);
                paperRepository.save(paper);
            }
            if (is != null) {
                is.close();
            }
            if (br != null) {
                br.close();
            }
        }
    }
}
