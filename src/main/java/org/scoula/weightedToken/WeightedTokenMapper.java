package org.scoula.weightedToken;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.scoula.weightedToken.dto.TokenScore;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@Mapper
public interface WeightedTokenMapper {

    //article_tokens 조회
    int selectTotalDocs();

    // ✅ @Param으로 totalDocs 이름을 명시 (MyBatis XML에서 #{totalDocs}로 정확히 매핑됨)
    List<TokenScore> selectAllTFIDF(@Param("totalDocs") int totalDocs);

    //기사 발행일자와 일일변동률 매칭
    Map<String, Object> selectNextAvailableVolatility(LocalDate publishDateTime);

    //토큰과 가중치 저장
    void insertKeyword(@Param("keyword") String keyword,@Param("weight") double weight);

}