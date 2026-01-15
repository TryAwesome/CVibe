/*
Query Builder - 查询构建器
================================================================================

构建 Elasticsearch 查询 DSL
*/

package search

// QueryBuilder 查询构建器
type QueryBuilder struct {
	must    []map[string]interface{}
	should  []map[string]interface{}
	filter  []map[string]interface{}
	mustNot []map[string]interface{}
}

// NewQueryBuilder 创建查询构建器
func NewQueryBuilder() *QueryBuilder {
	return &QueryBuilder{}
}

// Must 添加 must 条件
func (b *QueryBuilder) Must(query map[string]interface{}) *QueryBuilder {
	b.must = append(b.must, query)
	return b
}

// Should 添加 should 条件
func (b *QueryBuilder) Should(query map[string]interface{}) *QueryBuilder {
	b.should = append(b.should, query)
	return b
}

// Filter 添加 filter 条件
func (b *QueryBuilder) Filter(query map[string]interface{}) *QueryBuilder {
	b.filter = append(b.filter, query)
	return b
}

// MustNot 添加 must_not 条件
func (b *QueryBuilder) MustNot(query map[string]interface{}) *QueryBuilder {
	b.mustNot = append(b.mustNot, query)
	return b
}

// MatchQuery 构建 match 查询
func MatchQuery(field, query string) map[string]interface{} {
	return map[string]interface{}{
		"match": map[string]interface{}{
			field: query,
		},
	}
}

// TermQuery 构建 term 查询
func TermQuery(field string, value interface{}) map[string]interface{} {
	return map[string]interface{}{
		"term": map[string]interface{}{
			field: value,
		},
	}
}

// RangeQuery 构建 range 查询
func RangeQuery(field string, gte, lte interface{}) map[string]interface{} {
	rangeMap := make(map[string]interface{})
	if gte != nil {
		rangeMap["gte"] = gte
	}
	if lte != nil {
		rangeMap["lte"] = lte
	}
	return map[string]interface{}{
		"range": map[string]interface{}{
			field: rangeMap,
		},
	}
}

// Build 构建最终查询
func (b *QueryBuilder) Build() map[string]interface{} {
	boolQuery := make(map[string]interface{})
	
	if len(b.must) > 0 {
		boolQuery["must"] = b.must
	}
	if len(b.should) > 0 {
		boolQuery["should"] = b.should
	}
	if len(b.filter) > 0 {
		boolQuery["filter"] = b.filter
	}
	if len(b.mustNot) > 0 {
		boolQuery["must_not"] = b.mustNot
	}
	
	return map[string]interface{}{
		"query": map[string]interface{}{
			"bool": boolQuery,
		},
	}
}
