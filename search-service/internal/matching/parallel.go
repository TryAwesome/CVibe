/*
Parallel Processing - 并行计算工具
================================================================================

高性能并行处理
*/

package matching

import (
	"context"
	"runtime"
	"sync"
)

// ParallelProcessor 并行处理器
type ParallelProcessor struct {
	workerCount int
}

// NewParallelProcessor 创建并行处理器
func NewParallelProcessor() *ParallelProcessor {
	return &ParallelProcessor{
		workerCount: runtime.NumCPU(),
	}
}

// ProcessBatch 批量并行处理
func (p *ParallelProcessor) ProcessBatch[T any, R any](
	ctx context.Context,
	items []T,
	processor func(T) R,
) []R {
	results := make([]R, len(items))
	
	// 创建任务通道
	tasks := make(chan struct {
		index int
		item  T
	}, len(items))
	
	// 启动 worker
	var wg sync.WaitGroup
	for i := 0; i < p.workerCount; i++ {
		wg.Add(1)
		go func() {
			defer wg.Done()
			for task := range tasks {
				select {
				case <-ctx.Done():
					return
				default:
					results[task.index] = processor(task.item)
				}
			}
		}()
	}
	
	// 分发任务
	for i, item := range items {
		tasks <- struct {
			index int
			item  T
		}{i, item}
	}
	close(tasks)
	
	wg.Wait()
	return results
}

// WorkerPool 工作池
type WorkerPool struct {
	workers int
	tasks   chan func()
	wg      sync.WaitGroup
}

// NewWorkerPool 创建工作池
func NewWorkerPool(workers int) *WorkerPool {
	p := &WorkerPool{
		workers: workers,
		tasks:   make(chan func(), 100),
	}
	p.start()
	return p
}

func (p *WorkerPool) start() {
	for i := 0; i < p.workers; i++ {
		go func() {
			for task := range p.tasks {
				task()
			}
		}()
	}
}

// Submit 提交任务
func (p *WorkerPool) Submit(task func()) {
	p.wg.Add(1)
	p.tasks <- func() {
		defer p.wg.Done()
		task()
	}
}

// Wait 等待所有任务完成
func (p *WorkerPool) Wait() {
	p.wg.Wait()
}

// Close 关闭工作池
func (p *WorkerPool) Close() {
	close(p.tasks)
}
