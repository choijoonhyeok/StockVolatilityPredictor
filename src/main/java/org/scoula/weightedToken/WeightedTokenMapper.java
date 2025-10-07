package org.scoula.weightedToken;

import org.apache.ibatis.annotations.Mapper;
import org.scoula.weightedToken.dto.TokenScore;

import java.util.List;

@Mapper
public interface WeightedTokenMapper {
    int selectTotalDocs();

    // totalDocs를 SQL에서 IDF 계산용으로 넘겨줌
    List<TokenScore> selectAllTFIDF(int totalDocs);
}
