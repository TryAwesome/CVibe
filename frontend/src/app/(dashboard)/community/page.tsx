"use client";

import { useState } from "react";
import { Button } from "@/components/ui/button";
import { Card, CardContent, CardHeader, CardFooter } from "@/components/ui/card";
import { Input } from "@/components/ui/input";
import { Badge } from "@/components/ui/badge";
import { Avatar, AvatarFallback, AvatarImage } from "@/components/ui/avatar"; // I need to create Avatar component
import { Heart, MessageCircle, Share2, MoreHorizontal, Send } from "lucide-react";
import { cn } from "@/lib/utils";

// Mock Posts
const INITIAL_POSTS = [
  {
    id: 1,
    author: "Alice Chen",
    role: "Frontend Developer",
    content: "Just landed a job at Google using CVibe! The AI interview practice really helped me calm my nerves. Highly recommend the Mock Interview module.",
    time: "2 hours ago",
    likes: 24,
    comments: 5,
    liked: false
  },
  {
    id: 2,
    author: "Bob Smith",
    role: "Data Scientist",
    content: "Has anyone tried the new Resume Builder? Is it better than Overleaf?",
    time: "4 hours ago",
    likes: 8,
    comments: 12,
    liked: true
  }
];

export default function CommunityPage() {
  const [posts, setPosts] = useState(INITIAL_POSTS);
  const [newPost, setNewPost] = useState("");

  const handlePost = () => {
    if (!newPost.trim()) return;
    const post = {
        id: Date.now(),
        author: "Demo User",
        role: "Aspiring Engineer",
        content: newPost,
        time: "Just now",
        likes: 0,
        comments: 0,
        liked: false
    };
    setPosts([post, ...posts]);
    setNewPost("");
  };

  const toggleLike = (id: number) => {
    setPosts(posts.map(p => {
        if (p.id === id) {
            return { ...p, liked: !p.liked, likes: p.liked ? p.likes - 1 : p.likes + 1 };
        }
        return p;
    }));
  };

  return (
    <div className="h-full p-8 max-w-3xl mx-auto space-y-8 animate-in fade-in duration-500 overflow-y-auto">
        <div className="space-y-2">
            <h2 className="text-3xl font-bold tracking-tight">Community</h2>
            <p className="text-muted-foreground">Connect with other job seekers and share your journey.</p>
        </div>

        {/* Create Post */}
        <Card className="border-0 shadow-sm bg-card">
            <CardContent className="p-6">
                <div className="flex gap-4">
                    <div className="h-10 w-10 rounded-full bg-primary/10 flex items-center justify-center font-bold text-primary shrink-0">
                        DU
                    </div>
                    <div className="flex-1 space-y-4">
                        <Input 
                            placeholder="What's on your mind?" 
                            className="bg-muted/30 border-0 focus-visible:ring-1"
                            value={newPost}
                            onChange={(e) => setNewPost(e.target.value)}
                        />
                        <div className="flex justify-end">
                            <Button onClick={handlePost} disabled={!newPost.trim()}>
                                Post <Send className="ml-2 h-4 w-4" />
                            </Button>
                        </div>
                    </div>
                </div>
            </CardContent>
        </Card>

        {/* Feed */}
        <div className="space-y-6">
            {posts.map((post) => (
                <Card key={post.id} className="border-0 shadow-sm bg-card">
                    <CardHeader className="flex flex-row items-start justify-between space-y-0 pb-2">
                        <div className="flex gap-3">
                            <div className="h-10 w-10 rounded-full bg-slate-200 flex items-center justify-center font-bold text-slate-600 shrink-0">
                                {post.author[0]}
                            </div>
                            <div>
                                <h4 className="font-semibold text-sm">{post.author}</h4>
                                <p className="text-xs text-muted-foreground">{post.role} • {post.time}</p>
                            </div>
                        </div>
                        <Button variant="ghost" size="icon" className="h-8 w-8">
                            <MoreHorizontal className="h-4 w-4" />
                        </Button>
                    </CardHeader>
                    <CardContent className="pb-2">
                        <p className="text-sm leading-relaxed whitespace-pre-wrap">{post.content}</p>
                    </CardContent>
                    <CardFooter className="pt-2 pb-4">
                        <div className="flex items-center gap-6">
                            <Button 
                                variant="ghost" 
                                size="sm" 
                                className={cn("gap-2 text-muted-foreground", post.liked && "text-red-500 hover:text-red-600")}
                                onClick={() => toggleLike(post.id)}
                            >
                                <Heart className={cn("h-4 w-4", post.liked && "fill-current")} /> {post.likes}
                            </Button>
                            <Button variant="ghost" size="sm" className="gap-2 text-muted-foreground">
                                <MessageCircle className="h-4 w-4" /> {post.comments}
                            </Button>
                            <Button variant="ghost" size="sm" className="gap-2 text-muted-foreground ml-auto">
                                <Share2 className="h-4 w-4" /> Share
                            </Button>
                        </div>
                    </CardFooter>
                </Card>
            ))}
        </div>
    </div>
  );
}
