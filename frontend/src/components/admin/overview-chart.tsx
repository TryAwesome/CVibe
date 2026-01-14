"use client";

import { useMemo } from "react";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { MetricType } from "./admin-stats";
import { motion } from "framer-motion";

interface OverviewChartProps {
  type: MetricType;
}

// Mock Data Generators
const generateData = (type: MetricType) => {
  const days = ["Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun"];
  
  switch (type) {
    case "users":
      return days.map((day, i) => ({
        day,
        value: 12000 + (i * 120) + ((i % 2 === 0) ? 50 : -20) // Deterministic pattern
      }));
    case "jobs":
      return days.map((day, i) => ({
        day,
        value: 2000 + (i * 45) + ((i * 17) % 100)
      }));
    case "tokens":
      return days.map((day, i) => ({
        day,
        value: 40 + (i * 0.8) + Math.sin(i) * 2 // Deterministic sine wave
      }));
    case "health":
      // For health, maybe we show response time in ms
      return days.map((day, i) => ({
        day,
        value: 45 + ((i * 23) % 20)
      }));
    default:
      return [];
  }
};

const getTitle = (type: MetricType) => {
  switch (type) {
    case "users": return "User Growth (Last 7 Days)";
    case "jobs": return "Jobs Posted (Last 7 Days)";
    case "tokens": return "Token Usage (Millions)";
    case "health": return "Avg. System Response Time (ms)";
  }
};

export function OverviewChart({ type }: OverviewChartProps) {
  const data = useMemo(() => generateData(type), [type]);
  
  // Chart dimensions
  const height = 300;
  const width = 800;
  const padding = 40;
  
  const maxValue = Math.max(...data.map(d => d.value)) * 1.1;
  const minValue = Math.min(...data.map(d => d.value)) * 0.9;
  
  const getX = (index: number) => padding + (index * ((width - padding * 2) / (data.length - 1)));
  const getY = (value: number) => height - padding - ((value - minValue) / (maxValue - minValue)) * (height - padding * 2);

  const points = data.map((d, i) => `${getX(i)},${getY(d.value)}`).join(" ");
  const fillPath = `${points} ${width - padding},${height - padding} ${padding},${height - padding}`;

  return (
    <Card className="col-span-4">
      <CardHeader>
        <CardTitle>{getTitle(type)}</CardTitle>
      </CardHeader>
      <CardContent className="pl-2">
        <div className="h-[300px] w-full">
            <svg viewBox={`0 0 ${width} ${height}`} className="w-full h-full overflow-visible">
                {/* Grid Lines */}
                {[0, 1, 2, 3, 4].map((i) => {
                    const y = height - padding - (i * (height - padding * 2) / 4);
                    return (
                        <g key={i}>
                            <line 
                                x1={padding} 
                                y1={y} 
                                x2={width - padding} 
                                y2={y} 
                                stroke="currentColor" 
                                strokeOpacity={0.1} 
                            />
                            <text 
                                x={padding - 10} 
                                y={y + 4} 
                                textAnchor="end" 
                                fontSize="10" 
                                fill="currentColor"
                                className="text-muted-foreground"
                            >
                                {Math.round(minValue + (i * (maxValue - minValue) / 4))}
                            </text>
                        </g>
                    )
                })}

                {/* X Axis Labels */}
                {data.map((d, i) => (
                    <text 
                        key={i}
                        x={getX(i)} 
                        y={height - 10} 
                        textAnchor="middle" 
                        fontSize="12" 
                        fill="currentColor"
                        className="text-muted-foreground"
                    >
                        {d.day}
                    </text>
                ))}

                {/* Area Fill */}
                <motion.path
                    initial={{ opacity: 0 }}
                    animate={{ opacity: 0.2 }}
                    transition={{ duration: 0.5 }}
                    d={`M${padding},${height - padding} ${fillPath}`}
                    fill="currentColor"
                    className="text-primary"
                />

                {/* Line */}
                <motion.path
                    initial={{ pathLength: 0 }}
                    animate={{ pathLength: 1 }}
                    transition={{ duration: 1, ease: "easeInOut" }}
                    d={`M${points}`}
                    fill="none"
                    stroke="currentColor"
                    strokeWidth="2"
                    className="text-primary"
                />

                {/* Points */}
                {data.map((d, i) => (
                    <motion.circle
                        key={i}
                        cx={getX(i)}
                        cy={getY(d.value)}
                        r="4"
                        fill="currentColor"
                        className="text-background stroke-primary stroke-2"
                        initial={{ scale: 0 }}
                        animate={{ scale: 1 }}
                        transition={{ delay: 0.5 + (i * 0.1) }}
                    />
                ))}
            </svg>
        </div>
      </CardContent>
    </Card>
  );
}
