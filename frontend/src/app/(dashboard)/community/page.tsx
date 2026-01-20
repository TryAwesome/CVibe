"use client";

import { useState, useEffect } from "react";
import { Button } from "@/components/ui/button";
import { Card, CardContent, CardHeader, CardFooter, CardTitle } from "@/components/ui/card";
import { Input } from "@/components/ui/input";
import { Avatar, AvatarFallback, AvatarImage } from "@/components/ui/avatar";
import { Heart, MessageCircle, Share2, MoreHorizontal, Send, Search, Hash, MessageSquare, ChevronDown, ChevronUp, Loader2, AlertCircle, Users } from "lucide-react";
import { cn } from "@/lib/utils";
import { Textarea } from "@/components/ui/textarea";
import { api, Post, Comment } from "@/lib/api";
import { useAuth } from "@/lib/contexts/auth-context";

export default function CommunityPage() {
  const { user, isAuthenticated, isLoading: authLoading } = useAuth();
  
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  
  const [posts, setPosts] = useState<Post[]>([]);
  const [expandedPostId, setExpandedPostId] = useState<string | null>(null);
  const [postComments, setPostComments] = useState<Record<string, Comment[]>>({});
  const [newPost, setNewPost] = useState("");
  const [searchQuery, setSearchQuery] = useState("");
  const [newComment, setNewComment] = useState("");
  const [isPosting, setIsPosting] = useState(false);

  // Fetch posts on mount
  useEffect(() => {
    if (!authLoading && isAuthenticated) {
      fetchPosts();
    } else if (!authLoading && !isAuthenticated) {
      setIsLoading(false);
    }
  }, [authLoading, isAuthenticated]);

  const fetchPosts = async () => {
    setIsLoading(true);
    setError(null);
    try {
      const res = await api.getFeed();
      if (res.success && res.data) {
        setPosts(res.data.content || []);
      }
    } catch (err) {
      console.error('Error fetching posts:', err);
      setError('Failed to load posts');
    } finally {
      setIsLoading(false);
    }
  };

  const handlePost = async () => {
    if (!newPost.trim()) return;
    setIsPosting(true);
    try {
      const res = await api.createPost({ content: newPost, category: "DISCUSSION" });
      if (res.success && res.data) {
        setPosts([res.data, ...posts]);
        setNewPost("");
      } else {
        setError(res.error || 'Failed to create post');
      }
    } catch (err) {
      setError('Failed to create post');
    } finally {
      setIsPosting(false);
    }
  };

  const handleSendComment = async (postId: string) => {
    if (!newComment.trim()) return;
    try {
      const res = await api.createComment(postId, { content: newComment });
      if (res.success && res.data) {
        setPostComments(prev => ({
          ...prev,
          [postId]: [...(prev[postId] || []), res.data!]
        }));
        setPosts(prev => prev.map(p => 
          p.id === postId ? { ...p, commentsCount: p.commentsCount + 1 } : p
        ));
        setNewComment("");
      }
    } catch (err) {
      console.error('Error creating comment:', err);
    }
  };

  const toggleLike = async (e: React.MouseEvent, postId: string) => {
    e.stopPropagation();
    const post = posts.find(p => p.id === postId);
    if (!post) return;

    try {
      if (post.isLiked) {
        await api.unlikePost(postId);
        setPosts(prev => prev.map(p => 
          p.id === postId ? { ...p, isLiked: false, likesCount: p.likesCount - 1 } : p
        ));
      } else {
        await api.likePost(postId);
        setPosts(prev => prev.map(p => 
          p.id === postId ? { ...p, isLiked: true, likesCount: p.likesCount + 1 } : p
        ));
      }
    } catch (err) {
      console.error('Error toggling like:', err);
    }
  };

  const toggleComments = async (postId: string) => {
    if (expandedPostId === postId) {
      setExpandedPostId(null);
    } else {
      setExpandedPostId(postId);
      // Fetch comments if not already loaded
      if (!postComments[postId]) {
        try {
          const res = await api.getComments(postId);
          if (res.success && res.data) {
            setPostComments(prev => ({ ...prev, [postId]: res.data! }));
          }
        } catch (err) {
          console.error('Error fetching comments:', err);
        }
      }
    }
  };

  const handleSearch = async () => {
    if (!searchQuery.trim()) {
      fetchPosts();
      return;
    }
    try {
      const res = await api.searchPosts(searchQuery);
      if (res.success && res.data) {
        setPosts(res.data);
      }
    } catch (err) {
      console.error('Error searching:', err);
    }
  };

  const formatTime = (dateStr: string) => {
    const date = new Date(dateStr);
    const now = new Date();
    const diffMs = now.getTime() - date.getTime();
    const diffMins = Math.floor(diffMs / (1000 * 60));
    
    if (diffMins < 1) return 'Just now';
    if (diffMins < 60) return `${diffMins} mins ago`;
    const diffHours = Math.floor(diffMins / 60);
    if (diffHours < 24) return `${diffHours} hours ago`;
    const diffDays = Math.floor(diffHours / 24);
    if (diffDays < 7) return `${diffDays} days ago`;
    return date.toLocaleDateString();
  };

  // Loading state
  if (authLoading || isLoading) {
    return (
      <div className="h-full flex items-center justify-center">
        <Loader2 className="h-8 w-8 animate-spin text-muted-foreground" />
      </div>
    );
  }

  // Not authenticated
  if (!isAuthenticated) {
    return (
      <div className="h-full flex flex-col items-center justify-center gap-4">
        <AlertCircle className="h-12 w-12 text-muted-foreground" />
        <p className="text-muted-foreground">Please log in to access the community</p>
      </div>
    );
  }

  return (
    <div className="h-full p-4 md:p-6 lg:p-8 flex flex-col items-center overflow-y-auto">
        <div className="w-full max-w-5xl flex flex-col space-y-6">
            
            {/* Header / Search */}
            <div className="flex items-center gap-4 bg-card p-4 rounded-lg border shadow-sm w-full">
                <div className="relative flex-1">
                    <Search className="absolute left-3 top-2.5 h-4 w-4 text-muted-foreground" />
                    <Input 
                        placeholder="Search topics, posts, or people..." 
                        className="pl-9 bg-muted/20"
                        value={searchQuery}
                        onChange={(e) => setSearchQuery(e.target.value)}
                        onKeyDown={(e) => e.key === 'Enter' && handleSearch()}
                    />
                </div>
                <Button variant="outline" size="icon" title="Trending" onClick={async () => {
                  try {
                    const res = await api.getTrendingPosts();
                    if (res.success && res.data) {
                      setPosts(res.data);
                    }
                  } catch (err) {
                    console.error('Error:', err);
                  }
                }}>
                    <Hash className="h-4 w-4" />
                </Button>
            </div>

            {error && (
              <div className="bg-destructive/10 text-destructive p-4 rounded-lg flex items-center gap-2">
                <AlertCircle className="h-4 w-4" />
                {error}
              </div>
            )}

            {/* Create Post */}
            <Card className="border shadow-sm bg-card w-full">
                <CardContent className="p-4">
                    <div className="flex gap-4">
                        <Avatar>
                            <AvatarFallback className="bg-primary/10 text-primary font-bold">
                              {(user?.nickname || user?.email || 'U')[0].toUpperCase()}
                            </AvatarFallback>
                        </Avatar>
                        <div className="flex-1 space-y-3">
                            <Textarea 
                                placeholder="What's on your mind? Share with the community..." 
                                className="bg-muted/30 border-0 focus-visible:ring-1 min-h-[80px] resize-none"
                                value={newPost}
                                onChange={(e) => setNewPost(e.target.value)}
                            />
                            <div className="flex justify-between items-center">
                                <div className="text-xs text-muted-foreground flex gap-2">
                                    <Button variant="ghost" size="sm" className="h-6 px-2 text-xs">Image</Button>
                                    <Button variant="ghost" size="sm" className="h-6 px-2 text-xs">Poll</Button>
                                </div>
                                <Button size="sm" onClick={handlePost} disabled={!newPost.trim() || isPosting}>
                                    {isPosting && <Loader2 className="mr-2 h-3 w-3 animate-spin" />}
                                    Post <Send className="ml-2 h-3 w-3" />
                                </Button>
                            </div>
                        </div>
                    </div>
                </CardContent>
            </Card>

            {/* Posts Feed */}
            {posts.length === 0 ? (
              <div className="text-center py-12">
                <Users className="h-16 w-16 mx-auto text-muted-foreground mb-4" />
                <h3 className="text-xl font-semibold mb-2">No Posts Yet</h3>
                <p className="text-muted-foreground">Be the first to share something with the community!</p>
              </div>
            ) : (
              <div className="space-y-4 w-full pb-8">
                {posts.map((post) => (
                    <Card 
                        key={post.id} 
                        className={cn(
                            "border shadow-sm bg-card transition-all",
                            expandedPostId === post.id && "ring-1 ring-primary/20"
                        )}
                    >
                        <CardHeader className="flex flex-row items-start justify-between space-y-0 pb-2 p-4">
                            <div className="flex gap-3">
                                <Avatar className="h-10 w-10">
                                    <AvatarFallback className="bg-slate-200 text-slate-600 font-bold">
                                      {(post.authorName || 'U')[0].toUpperCase()}
                                    </AvatarFallback>
                                </Avatar>
                                <div>
                                    <h4 className="font-semibold text-sm hover:underline cursor-pointer">{post.authorName || 'Unknown'}</h4>
                                    <p className="text-xs text-muted-foreground">
                                      {post.authorRole && `${post.authorRole} • `}{formatTime(post.createdAt)}
                                    </p>
                                </div>
                            </div>
                            <Button variant="ghost" size="icon" className="h-8 w-8">
                                <MoreHorizontal className="h-4 w-4" />
                            </Button>
                        </CardHeader>
                        
                        <CardContent className="pb-2 px-4 cursor-pointer" onClick={() => toggleComments(post.id)}>
                            <p className="text-sm leading-relaxed whitespace-pre-wrap">{post.content}</p>
                        </CardContent>
                        
                        <CardFooter className="pt-2 pb-3 px-4 flex flex-col items-start border-t bg-muted/5 mt-2">
                            {/* Action Buttons */}
                            <div className="flex items-center gap-4 w-full">
                                <Button 
                                    variant="ghost" 
                                    size="sm" 
                                    className={cn("gap-1.5 h-8 text-muted-foreground", post.isLiked && "text-red-500 hover:text-red-600")}
                                    onClick={(e) => toggleLike(e, post.id)}
                                >
                                    <Heart className={cn("h-4 w-4", post.isLiked && "fill-current")} /> 
                                    <span className="text-xs">{post.likesCount}</span>
                                </Button>
                                <Button 
                                    variant="ghost" 
                                    size="sm" 
                                    className={cn("gap-1.5 h-8 text-muted-foreground", expandedPostId === post.id && "text-primary bg-primary/10")}
                                    onClick={() => toggleComments(post.id)}
                                >
                                    <MessageCircle className="h-4 w-4" /> 
                                    <span className="text-xs">{post.commentsCount}</span>
                                </Button>
                                <Button variant="ghost" size="sm" className="gap-1.5 h-8 text-muted-foreground ml-auto">
                                    <Share2 className="h-4 w-4" />
                                </Button>
                            </div>

                            {/* Expanded Comments Section */}
                            {expandedPostId === post.id && (
                                <div className="w-full pt-4 animate-in slide-in-from-top-2 duration-200">
                                    <div className="space-y-4 mb-4 pl-2 border-l-2 border-muted">
                                        {(postComments[post.id] || []).length > 0 ? (
                                            postComments[post.id].map((comment) => (
                                                <div key={comment.id} className="flex gap-3">
                                                    <Avatar className="h-6 w-6">
                                                        <AvatarFallback className="bg-primary/10 text-primary text-[10px]">
                                                          {(comment.authorName || 'U')[0].toUpperCase()}
                                                        </AvatarFallback>
                                                    </Avatar>
                                                    <div className="flex-1">
                                                        <div className="flex items-baseline gap-2">
                                                            <span className="text-xs font-semibold">{comment.authorName || 'Unknown'}</span>
                                                            <span className="text-[10px] text-muted-foreground">{formatTime(comment.createdAt)}</span>
                                                        </div>
                                                        <p className="text-sm text-foreground/90">{comment.content}</p>
                                                    </div>
                                                </div>
                                            ))
                                        ) : (
                                            <p className="text-xs text-muted-foreground italic">No comments yet. Be the first!</p>
                                        )}
                                    </div>
                                    
                                    <div className="flex gap-2">
                                        <Avatar className="h-8 w-8">
                                            <AvatarFallback className="bg-primary/10 text-primary text-xs font-bold">
                                              {(user?.nickname || user?.email || 'U')[0].toUpperCase()}
                                            </AvatarFallback>
                                        </Avatar>
                                        <div className="flex-1 flex gap-2">
                                            <Input 
                                                placeholder="Write a comment..." 
                                                className="h-8 text-xs"
                                                value={newComment}
                                                onChange={(e) => setNewComment(e.target.value)}
                                                onKeyDown={(e) => e.key === 'Enter' && handleSendComment(post.id)}
                                            />
                                            <Button size="sm" className="h-8 w-8 p-0" onClick={() => handleSendComment(post.id)} disabled={!newComment.trim()}>
                                                <Send className="h-3 w-3" />
                                            </Button>
                                        </div>
                                    </div>
                                </div>
                            )}
                        </CardFooter>
                    </Card>
                ))}
              </div>
            )}
        </div>
    </div>
  );
}
