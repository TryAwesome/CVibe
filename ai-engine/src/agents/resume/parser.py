"""
Resume Parser - 从 PDF/图像简历提取结构化数据
================================================================================

支持:
- PDF 文件解析
- 图像 OCR 解析
- 使用 LLM 进行结构化提取
"""

import io
import json
from typing import Optional, Union
from dataclasses import dataclass, field

from PyPDF2 import PdfReader

from ...llm import LLMClient, VisionClient


@dataclass
class ParsedResume:
    """解析后的简历数据"""
    raw_text: str = ""
    name: str = ""
    email: str = ""
    phone: str = ""
    summary: str = ""
    education: list[dict] = field(default_factory=list)
    work_experience: list[dict] = field(default_factory=list)
    projects: list[dict] = field(default_factory=list)
    skills: list[str] = field(default_factory=list)
    achievements: list[str] = field(default_factory=list)


RESUME_PARSE_PROMPT = """你是一个简历解析专家。请从以下简历文本中提取结构化信息。

请提取以下字段，严格按照 JSON 格式输出：
{
    "name": "姓名",
    "email": "邮箱",
    "phone": "电话",
    "summary": "个人简介/自我评价",
    "education": [
        {
            "school": "学校名称",
            "degree": "学位",
            "field": "专业",
            "start_date": "YYYY-MM",
            "end_date": "YYYY-MM 或 present"
        }
    ],
    "work_experience": [
        {
            "company": "公司名称",
            "title": "职位",
            "start_date": "YYYY-MM",
            "end_date": "YYYY-MM 或 present",
            "description": "工作描述",
            "is_current": true/false
        }
    ],
    "skills": ["技能1", "技能2", ...],
    "achievements": ["成就1", "成就2", ...]
}

简历内容：
{resume_text}

请只输出 JSON，不要有其他内容。"""


VISION_PARSE_PROMPT = """请仔细分析这份简历图片，提取所有结构化信息。

输出格式为 JSON：
{
    "name": "姓名",
    "email": "邮箱",
    "phone": "电话",
    "summary": "个人简介",
    "education": [{"school": "学校", "degree": "学位", "field": "专业", "start_date": "YYYY-MM", "end_date": "YYYY-MM"}],
    "work_experience": [{"company": "公司", "title": "职位", "start_date": "YYYY-MM", "end_date": "YYYY-MM", "description": "描述", "is_current": false}],
    "skills": ["技能列表"],
    "achievements": ["成就列表"]
}

请只输出 JSON。"""


class ResumeParser:
    """
    简历解析器
    
    功能：
    - 从 PDF 提取文本并解析
    - 从图像 OCR 提取并解析
    - 使用 LLM 进行结构化解析
    """

    def __init__(
        self, 
        llm_client: Optional[LLMClient] = None,
        vision_client: Optional[VisionClient] = None
    ):
        self.llm = llm_client
        self.vision = vision_client

    def parse_pdf_bytes(self, pdf_bytes: bytes) -> ParsedResume:
        """
        解析 PDF 字节数据
        
        Args:
            pdf_bytes: PDF 文件的字节内容
            
        Returns:
            解析后的简历数据
        """
        # 提取 PDF 文本
        raw_text = self._extract_text_from_pdf_bytes(pdf_bytes)
        
        if not raw_text.strip():
            # PDF 可能是图像格式，尝试 OCR
            if self.vision:
                return self._parse_pdf_as_image(pdf_bytes)
            return ParsedResume(raw_text="[无法提取文本]")
        
        # 使用 LLM 解析
        return self._parse_with_llm(raw_text)

    def parse_pdf(self, pdf_path: str) -> ParsedResume:
        """解析 PDF 文件路径"""
        with open(pdf_path, 'rb') as f:
            return self.parse_pdf_bytes(f.read())

    def parse_image(self, image_data: Union[str, bytes]) -> ParsedResume:
        """
        解析图像简历
        
        Args:
            image_data: 图像 URL、base64 或字节数据
        """
        if not self.vision:
            return ParsedResume(raw_text="[需要视觉模型进行图像解析]")
        
        response = self.vision.analyze_image(image_data, VISION_PARSE_PROMPT)
        return self._parse_json_response(response, "[图像简历]")

    def parse_text(self, text: str) -> ParsedResume:
        """直接解析文本"""
        return self._parse_with_llm(text)

    def _extract_text_from_pdf_bytes(self, pdf_bytes: bytes) -> str:
        """从 PDF 字节提取文本"""
        try:
            reader = PdfReader(io.BytesIO(pdf_bytes))
            text_parts = []
            for page in reader.pages:
                page_text = page.extract_text()
                if page_text:
                    text_parts.append(page_text)
            return "\n".join(text_parts)
        except Exception as e:
            return f"[PDF解析错误: {str(e)}]"

    def _parse_pdf_as_image(self, pdf_bytes: bytes) -> ParsedResume:
        """将 PDF 转为图像进行 OCR 解析"""
        try:
            from pdf2image import convert_from_bytes
            images = convert_from_bytes(pdf_bytes, first_page=1, last_page=3)
            
            all_text = []
            for img in images:
                # 转为 base64
                import base64
                buffer = io.BytesIO()
                img.save(buffer, format='PNG')
                img_b64 = base64.b64encode(buffer.getvalue()).decode()
                
                response = self.vision.analyze_image(
                    f"data:image/png;base64,{img_b64}",
                    VISION_PARSE_PROMPT
                )
                all_text.append(response)
            
            # 合并解析结果
            return self._parse_json_response(all_text[0] if all_text else "{}", "[PDF图像]")
        except ImportError:
            return ParsedResume(raw_text="[需要 pdf2image 库进行 PDF 图像解析]")
        except Exception as e:
            return ParsedResume(raw_text=f"[PDF图像解析错误: {str(e)}]")

    def _parse_with_llm(self, text: str) -> ParsedResume:
        """使用 LLM 解析简历文本"""
        if not self.llm:
            return ParsedResume(raw_text=text)
        
        prompt = RESUME_PARSE_PROMPT.format(resume_text=text[:8000])  # 限制长度
        
        messages = [
            {"role": "system", "content": "你是一个专业的简历解析助手。请严格按照要求的 JSON 格式输出。"},
            {"role": "user", "content": prompt}
        ]
        
        try:
            response = self.llm.chat(messages)
            return self._parse_json_response(response, text)
        except Exception as e:
            return ParsedResume(raw_text=text, summary=f"[LLM解析错误: {str(e)}]")

    def _parse_json_response(self, response: str, raw_text: str) -> ParsedResume:
        """解析 LLM 返回的 JSON"""
        try:
            # 尝试提取 JSON
            json_str = response
            if "```json" in response:
                json_str = response.split("```json")[1].split("```")[0]
            elif "```" in response:
                json_str = response.split("```")[1].split("```")[0]
            
            data = json.loads(json_str.strip())
            
            return ParsedResume(
                raw_text=raw_text,
                name=data.get("name", ""),
                email=data.get("email", ""),
                phone=data.get("phone", ""),
                summary=data.get("summary", ""),
                education=data.get("education", []),
                work_experience=data.get("work_experience", []),
                projects=data.get("projects", []),
                skills=data.get("skills", []),
                achievements=data.get("achievements", []),
            )
        except json.JSONDecodeError:
            return ParsedResume(raw_text=raw_text, summary="[JSON解析失败]")
