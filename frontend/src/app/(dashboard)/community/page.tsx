"use client";

import { useState } from "react";
import { Button } from "@/components/ui/button";
import { Card, CardContent, CardHeader, CardFooter, CardTitle } from "@/components/ui/card";
import { Input } from "@/components/ui/input";
import { Avatar, AvatarFallback, AvatarImage } from "@/components/ui/avatar";
import { Heart, MessageCircle, Share2, MoreHorizontal, Send, Search, Hash, MessageSquare, ChevronDown, ChevronUp } from "lucide-react";
import { cn } from "@/lib/utils";
import { Textarea } from "@/components/ui/textarea";

// Mock Data
const INITIAL_POSTS = [
  {
    id: 1,
    author: "Alice Chen",
    role: "Frontend Developer",
    content: "Just landed a job at Google using CVibe! The AI interview practice really helped me calm my nerves. Highly recommend the Mock Interview module.",
    time: "2 hours ago",
    likes: 24,
    commentsCount: 3,
    liked: false
  },
  {
    id: 2,
    author: "Bob Smith",
    role: "Data Scientist",
    content: "Has anyone tried the new Resume Builder? Is it better than Overleaf? I'm trying to decide if I should switch my LaTeX templates over. Also, does it support custom fonts?",
    time: "4 hours ago",
    likes: 8,
    commentsCount: 12,
    liked: true
  },
  {
    id: 3,
    author: "Carol Williams",
    role: "Product Manager",
    content: "Any tips for transitioning from Engineering to PM? I have an interview next week for a technical PM role.",
    time: "5 hours ago",
    likes: 15,
    commentsCount: 0,
    liked: false
  }
];

const MOCK_COMMENTS = [
    { id: 101, author: "David", role: "Software Engineer", content: "Congrats Alice! That's huge.", time: "1 hour ago" },
    { id: 102, author: "Eve", role: "Recruiter", content: "Google is lucky to have you!", time: "45 mins ago" },
    { id: 103, author: "Frank", role: "Student", content: "Which specific questions did you get?", time: "10 mins ago" },
];

export default function CommunityPage() {
  const [posts, setPosts] = useState(INITIAL_POSTS);
  const [expandedPostId, setExpandedPostId] = useState<number | null>(null);
  const [newPost, setNewPost] = useState("");
  const [searchQuery, setSearchQuery] = useState("");
  const [newComment, setNewComment] = useState("");

  const handlePost = () => {
    if (!newPost.trim()) return;
    const post = {
        id: Date.now(),
        author: "Demo User",
        role: "Aspiring Engineer",
        content: newPost,
        time: "Just now",
        likes: 0,
        commentsCount: 0,
        liked: false
    };
    setPosts([post, ...posts]);
    setNewPost("");
  };

  const handleSendComment = (postId: number) => {
      if (!newComment.trim()) return;
      // In a real app, add comment logic here
      console.log(`Comment on post ${postId}: ${newComment}`);
      setNewComment("");
  };

  const toggleLike = (e: React.MouseEvent, id: number) => {
    e.stopPropagation();
    setPosts(posts.map(p => {
        if (p.id === id) {
            return { ...p, liked: !p.liked, likes: p.liked ? p.likes - 1 : p.likes + 1 };
        }
        return p;
    }));
  };

  const toggleComments = (id: number) => {
      if (expandedPostId === id) {
          setExpandedPostId(null);
      } else {
          setExpandedPostId(id);
      }
  };

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
                    />
                </div>
                <Button variant="outline" size="icon" title="Trending">
                    <Hash className="h-4 w-4" />
                </Button>
            </div>

            {/* Create Post */}
            <Card className="border shadow-sm bg-card w-full">
                <CardContent className="p-4">
                    <div className="flex gap-4">
                        <Avatar>
                            <AvatarFallback className="bg-primary/10 text-primary font-bold">ME</AvatarFallback>
                        </Avatar>
                        <div className="flex-1 space-y-3">
                            <Input 
                                placeholder="What's on your mind? Share with the community..." 
                                className="bg-muted/30 border-0 focus-visible:ring-1 min-h-[50px]"
                                value={newPost}
                                onChange={(e) => setNewPost(e.target.value)}
                            />
                            <div className="flex justify-between items-center">
                                <div className="text-xs text-muted-foreground flex gap-2">
                                    <Button variant="ghost" size="sm" className="h-6 px-2 text-xs">Image</Button>
                                    <Button variant="ghost" size="sm" className="h-6 px-2 text-xs">Poll</Button>
                                </div>
                                <Button size="sm" onClick={handlePost} disabled={!newPost.trim()}>
                                    Post <Send className="ml-2 h-3 w-3" />
                                </Button>
                            </div>
                        </div>
                    </div>
                </CardContent>
            </Card>

            {/* Posts Feed */}
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
                                    <AvatarFallback className="bg-slate-200 text-slate-600 font-bold">{post.author[0]}</AvatarFallback>
                                </Avatar>
                                <div>
                                    <h4 className="font-semibold text-sm hover:underline cursor-pointer">{post.author}</h4>
                                    <p className="text-xs text-muted-foreground">{post.role} • {post.time}</p>
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
                                    className={cn("gap-1.5 h-8 text-muted-foreground", post.liked && "text-red-500 hover:text-red-600")}
                                    onClick={(e) => toggleLike(e, post.id)}
                                >
                                    <Heart className={cn("h-4 w-4", post.liked && "fill-current")} /> 
                                    <span className="text-xs">{post.likes}</span>
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
                                        {post.commentsCount > 0 ? (
                                            MOCK_COMMENTS.map((comment) => (
                                                <div key={comment.id} className="flex gap-3">
                                                    <Avatar className="h-6 w-6">
                                                        <AvatarFallback className="bg-primary/10 text-primary text-[10px]">{comment.author[0]}</AvatarFallback>
                                                    </Avatar>
                                                    <div className="flex-1">
                                                        <div className="flex items-baseline gap-2">
                                                            <span className="text-xs font-semibold">{comment.author}</span>
                                                            <span className="text-[10px] text-muted-foreground">{comment.time}</span>
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
                                            <AvatarFallback className="bg-primary/10 text-primary text-xs font-bold">ME</AvatarFallback>
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
        </div>
    </div>
  );
}
