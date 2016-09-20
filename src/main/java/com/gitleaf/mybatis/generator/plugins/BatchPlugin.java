package com.gitleaf.mybatis.generator.plugins;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.mybatis.generator.api.IntrospectedColumn;
import org.mybatis.generator.api.IntrospectedTable;
import org.mybatis.generator.api.PluginAdapter;
import org.mybatis.generator.api.dom.java.*;
import org.mybatis.generator.api.dom.xml.*;

import java.util.List;

/**
 * @author admin
 * @date 2016-09-20
 * mybatis-generator batch-plugin
 */
public class BatchPlugin extends PluginAdapter {

    public boolean validate(List<String> list) {
        return true;
    }


    /**
     * 为mybatis java客户端(xxxMapper.java)添加批量操作方法
     * @param anInterface
     * @param topLevelClass
     * @param introspectedTable
     * @return
     */
    @Override
    public boolean clientGenerated(Interface anInterface, TopLevelClass topLevelClass, IntrospectedTable introspectedTable) {


        addInsertBatchMethod(anInterface,topLevelClass,introspectedTable);


        return super.clientGenerated(anInterface, topLevelClass, introspectedTable);
    }

    private void addInsertBatchMethod(Interface interfaze, TopLevelClass topLevelClass, IntrospectedTable introspectedTable) {
        //introspectedTable.getTableType();
        //java模型 类型
        String modeType = introspectedTable.getBaseRecordType();


        // int insertBatch(List<?> datas)
        Method insertBatchListMethod = new Method();
        insertBatchListMethod.setName("insertBatch");
        FullyQualifiedJavaType rowsReturn = FullyQualifiedJavaType.getIntInstance();
        insertBatchListMethod.setReturnType(rowsReturn);
        Parameter listParam = new Parameter(new FullyQualifiedJavaType("java.util.List<"+modeType+">"),getModelVarName(introspectedTable)+"s");
        listParam.addAnnotation("@Param(\""+getModelVarName(introspectedTable)+"s\")");

        insertBatchListMethod.addParameter(listParam);

        interfaze.addMethod(insertBatchListMethod);
    }


    private String getModelVarName(IntrospectedTable introspectedTable){

        String modelType = introspectedTable.getBaseRecordType();

        String modelLastName  = modelType.substring(modelType.lastIndexOf(".")+1);

        //lower first char
        String varName = StringUtils.lowerCase(modelLastName.substring(0,1))+modelLastName.substring(1);

        return varName;
    }



    /**
     * 为mybatis sql映射文件添加对应方法的元素
     * @param document
     * @param introspectedTable
     * @return
     */
    @Override
    public boolean sqlMapDocumentGenerated(Document document, IntrospectedTable introspectedTable) {


        XmlElement root =  document.getRootElement();

        root.addElement(getInsertBatchListElement(introspectedTable));


        return super.sqlMapDocumentGenerated(document, introspectedTable);
    }


    private Element getInsertBatchListElement(IntrospectedTable introspectedTable) {

        String tableName = introspectedTable.getFullyQualifiedTable().getIntrospectedTableName();
        String varName =getModelVarName(introspectedTable);

        //<insert id="insertBatch" parameterType="java.util.Set"
        //     insert into table_name
        //     values
        //     <foreach collection='users' item='user' separator=",">
        //           (#{user.id} ,#{user.name},... )
        //     </foreach>
        XmlElement insertBatchSetElement = new XmlElement("insert");

        Attribute idAttribute = new Attribute("id","insertBatch");
        insertBatchSetElement.addAttribute(idAttribute);

        Attribute paramAttribute = new Attribute("parameterType","java.util.List");
        insertBatchSetElement.addAttribute(paramAttribute);

        TextElement insertTextElement = new TextElement("insert into "+tableName+" values");
        insertBatchSetElement.addElement(insertTextElement);

        XmlElement forEachElement = new XmlElement("foreach");
        forEachElement.addAttribute(new Attribute("collection",varName+"s"));
        forEachElement.addAttribute(new Attribute("item",varName));
        forEachElement.addAttribute(new Attribute("separator",","));

        List<IntrospectedColumn> columns = introspectedTable.getAllColumns();
        StringBuffer selectText = new StringBuffer("(");
        if(columns != null && columns.size() != 0){

            for (int i = 0; i< columns.size(); i++){
                IntrospectedColumn column = columns.get(i);

                selectText.append("#{"+varName+"."+column.getJavaProperty()+"}");
                if(i < columns.size() - 1){

                    selectText.append(",");
                }
            }
        }
        selectText.append(")");

        TextElement selectTextElement = new TextElement(selectText.toString());
        forEachElement.addElement(selectTextElement);

        insertBatchSetElement.addElement(forEachElement);


        return insertBatchSetElement;


    }
}
