package buaa.scse.relationclassification.controller;

import buaa.scse.relationclassification.RelationclassificationApplication;
import buaa.scse.relationclassification.entity.Paper;
import buaa.scse.relationclassification.repository.PaperRepository;
import buaa.scse.relationclassification.util.HttpRequest;

import org.apache.tomcat.util.buf.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.FileSystemResourceLoader;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.huaban.analysis.jieba.JiebaSegmenter;
import com.huaban.analysis.jieba.JiebaSegmenter.SegMode;
import com.huaban.analysis.jieba.WordDictionary;

import java.io.*;
import java.nio.charset.Charset;
import java.nio.file.Paths;
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
    
    HashMap<String, Integer> relationMapping = new HashMap() {
        {
            put("position", 0);
            put("describe", 1);
            put("part-whole", 2);
            put("cause", 3);
            put("effect", 4);
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
    
    public HelloController(){
    	
    }

    @RequestMapping(value = "/search")
    public String search(@RequestParam(value = "field", defaultValue = "wuli") String field,
                         @RequestParam(value = "keyword", defaultValue = "") String keyword,
                         Model model) {
    	List<Paper> papers = new ArrayList<>();
    	if (keyword == null || keyword.isEmpty()) {
    		model.addAttribute("papers", papers);
            model.addAttribute("field", field);
            model.addAttribute("keyword", keyword);
            return "index";
    	}
    	switch(field) {
    	case "wuli":case "huaxue":
    		papers = paperRepository.findAllByFieldTypeAndContentContains(mapping.get(field), keyword);
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
            break;
    	case "wulire":case "huaxuere":
    		String keywords = RelationclassificationApplication.segmenter.process(keyword, SegMode.SEARCH).toString();
    		keywords = keywords.replaceAll("\\[", "").replaceAll("\\]", "");
    		String[] tokens = keywords.split(", ");
    		String[] sents = new String[tokens.length/3];
    		for (int i = 0; i < tokens.length; i++) {
    			if (i % 3 == 0) {
    				sents[i/3] = tokens[i];
    			}
    		}
    		List<Integer> pos = new ArrayList<Integer>();
    		StringBuilder stb = new StringBuilder();
    		for (int i = 0; i < sents.length; i++) {
    			if (RelationclassificationApplication.mmSegment.getDictionary().contains(sents[i])) {
    				pos.add(i);
    			}
    			stb.append(sents[i]);
				stb.append(" ");
    		}
    		
    		String sr = null;
    		if (field.equals("wulire")) {
    			sr = HttpRequest.sendPost("http://10.10.20.158:5001/predict", 
        				"text="+pos.get(0)+" "+pos.get(1)+" "+stb.toString());
    		} else if (field.equals("huaxuere")) {
    			sr = HttpRequest.sendPost("http://10.10.20.158:5000/predict", 
        				"text="+pos.get(0)+" "+pos.get(1)+" "+stb.toString());
    		}
    		
    		System.out.println(StringUtils.join(sents));
    		System.out.println(pos);
    		int relationType = -1;
    		for (String key : relationMapping.keySet()) {
    		    if (sr.contains(key)) {
    		    	relationType = relationMapping.get(key);
    		    	break;
    		    }
    		}
    		stb = new StringBuilder();
    		for (int i = 0; i < sents.length; i++) {
    			if (RelationclassificationApplication.mmSegment.getDictionary().contains(sents[i])) {
    				pos.add(i);
    				stb.append("<font color='red'><strong>");
    				stb.append(sents[i]);
    				stb.append("</strong></font>");
    			} else if (symbol.contains(tokens[i]) && i==tokens.length-1) {
                    continue;
                } else {
    				stb.append(sents[i]);
    			}
    		}

    		Paper paper = new Paper();
    		paper.setContent(stb.toString());
    		paper.setEntityOnePosition(pos.get(0));
    		paper.setEntityTwoPosition(pos.get(1));
    		paper.setRelationType(relationType);
    		papers.add(paper);
    		model.addAttribute("papers", papers);
            model.addAttribute("field", field);
            model.addAttribute("keyword", keyword);
    	}
        return "index";
    }

    private List<Paper> deleteDuplicate(List<Paper> papers) {
        LinkedHashSet<Paper> set = new LinkedHashSet<Paper>(papers.size());
        set.addAll(papers);
        papers.clear();
        papers.addAll(set);
        return papers;
    }
    
    /*private StringBuilder appendStringBuilder(StringBuilder stringBuilder, String token) {
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
    }*/

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
