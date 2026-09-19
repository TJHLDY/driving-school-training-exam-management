package com.example.drivingschool.mapper;

import org.apache.ibatis.builder.xml.XMLMapperBuilder;
import org.apache.ibatis.io.Resources;
import org.apache.ibatis.session.Configuration;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class MapperXmlValidationTest {

    @Test
    void allMapperXmlFilesParseAndBind() throws Exception {
        Configuration configuration = new Configuration();
        configuration.getTypeAliasRegistry().registerAliases("com.example.drivingschool.entity");
        configuration.addMapper(AccountMapper.class);
        configuration.addMapper(EnrollmentMapper.class);
        configuration.addMapper(TrainingMapper.class);
        configuration.addMapper(ExamMapper.class);
        configuration.addMapper(WithdrawalMapper.class);

        List<String> resources = List.of("mapper/AccountMapper.xml", "mapper/EnrollmentMapper.xml",
                "mapper/TrainingMapper.xml", "mapper/ExamMapper.xml", "mapper/WithdrawalMapper.xml");
        for (String resource : resources) {
            try (InputStream input = Resources.getResourceAsStream(resource)) {
                new XMLMapperBuilder(input, configuration, resource, configuration.getSqlFragments()).parse();
            }
        }

        assertThat(configuration.hasStatement(
                "com.example.drivingschool.mapper.ExamMapper.gradeAnswers", false)).isTrue();
        assertThat(configuration.hasStatement(
                "com.example.drivingschool.mapper.TrainingMapper.countOverlaps", false)).isTrue();
        assertThat(configuration.hasStatement(
                "com.example.drivingschool.mapper.WithdrawalMapper.refund", false)).isTrue();
    }
}
