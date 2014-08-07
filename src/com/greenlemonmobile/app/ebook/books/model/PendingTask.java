package com.greenlemonmobile.app.ebook.books.model;

public class PendingTask implements Comparable<PendingTask> {
	public enum PendingTaskType {
		NextChapter,
		PreviousChapter,
		GotoBookmark,
		JumpNode,
		ApplyRuntimeSetting,
		Paginating,
		Cache,
		Kill
	}
	
	private final long when = System.nanoTime();
	
	public PendingTaskType type;

	public int chapterIndex = 0;
	
	public int pageIndex = 0;
	
    public int nodeIndex;
    public int nodeOffset;
    
    public String anchor;
    
    public PendingTask(PendingTaskType type) {
    	this.type = type;
    }

	@Override
	public int compareTo(PendingTask another) {
		return Long.valueOf(another.when).compareTo(when);
	};
}
