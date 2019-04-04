package com.jasongofen.test;

import com.jasongofen.client.SolrClient;
import com.jasongofen.config.SolrConfigProperties;
import com.jasongofen.util.ConvertUtil;
import com.jasongofen.util.TikaUtil;
import org.apache.solr.client.solrj.impl.HttpSolrClient;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrDocumentList;
import org.apache.solr.common.SolrInputDocument;
import org.apache.solr.common.params.MapSolrParams;
import org.apache.solr.common.params.SolrParams;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.io.FileInputStream;
import java.io.InputStream;
import java.util.*;

@RestController
@RequestMapping("/Solr")
public class SolrCURDTest {

    @Autowired
    private SolrConfigProperties solrConfigProperties;

    @GetMapping("/Add")
    public void solrAdd() throws Exception {

        // 设置文件路径
        List<String> files = new ArrayList<>();
        files.add("鹅鹅鹅.pdf");
        files.add("静夜思.docx");
        // 获取Solr客户端
        HttpSolrClient solr = SolrClient.getClient(solrConfigProperties.getServer());

        String prefix = "";
        for (String fi : files) {
            System.out.println(fi);
            // 取后缀名
            prefix = ConvertUtil.getFileSufix(fi);
            if (prefix.equalsIgnoreCase("txt") ||
                    prefix.equalsIgnoreCase("docx") ||
                    prefix.equalsIgnoreCase("doc") ||
                    prefix.equalsIgnoreCase("pdf")) {

                String[] fileInfo = fi.split("\\.");
                String content = "";
                // 获取文件流，取出文件内容
                InputStream inputStream = new FileInputStream(solrConfigProperties.getDir() + fi);
                if (prefix.equals("txt")) {
                    content = TikaUtil.txt2String(inputStream);
                } else if (prefix.equals("docx") || prefix.equals("doc") || prefix.equals("pdf")) {
                    content = TikaUtil.doc2String(inputStream);
                } else {
                    inputStream.close();
                }
                // 添加索引
                SolrInputDocument solrDoc = new SolrInputDocument();
                String formatDate = ConvertUtil.formatDate();
                // 执行添加 ps：如果id相同，则执行更新操作
                solrDoc.addField("id", UUID.randomUUID().toString().toUpperCase().replace("-", ""));
                solrDoc.addField("title", fileInfo[0]);
                solrDoc.addField("content", content);
                solrDoc.addField("filetype", prefix);
                solrDoc.addField("uploadtime", formatDate);
                solr.add(solrConfigProperties.getCore(), solrDoc);

            } else {
                continue;
            }
        }
        // 提交
        solr.commit(solrConfigProperties.getCore());
        solr.close();
    }

    @GetMapping("/Query")
    public SolrDocumentList solrQuery() throws Exception {

        HttpSolrClient solrClient = SolrClient.getClient(solrConfigProperties.getServer());
        // 定义查询条件
        Map<String, String> params = new HashMap<String, String>();
        params.put("q", "*:*");
        SolrParams mapSolrParams = new MapSolrParams(params);
        //执行查询 第一个参数是collection，就是我们在solr中创建的core
        QueryResponse response = solrClient.query(solrConfigProperties.getCore(), mapSolrParams);
        // 获取结果集
        SolrDocumentList results = response.getResults();
        for (SolrDocument result : results) {
            // SolrDocument 数据结构为Map
            System.out.println(result);
        }
        solrClient.close();
        return results;

    }

    @GetMapping("/Delete")
    public void solrDelete(@RequestParam("id") String id) throws Exception {

        HttpSolrClient solrClient = SolrClient.getClient(solrConfigProperties.getServer());
        // 通过id删除 执行要删除的collection（core）
        solrClient.deleteById(solrConfigProperties.getCore(), id);
        // 还可以通过查询条件删除
        // solrClient.deleteByQuery(solrConfigProperties.getCore(), "查询条件");
        // 提交删除
        solrClient.commit(solrConfigProperties.getCore());
        solrClient.close();

    }

}
