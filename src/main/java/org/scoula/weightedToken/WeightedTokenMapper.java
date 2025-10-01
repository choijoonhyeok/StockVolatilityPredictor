package org.scoula.weightedToken;

import org.apache.ibatis.annotations.Mapper;
import org.scoula.weightedToken.dto.TokenScore;

import java.util.List;

@Mapper
public interface WeightedTokenMapper {
    int selectTotalDocs();

    List<TokenScore> selectAllTFIDF(int totalDocs);
}
