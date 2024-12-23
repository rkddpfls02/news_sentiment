import requests
import re
import pandas as pd
from openpyxl import Workbook, load_workbook

# API endpoint for news content retrieval
url = "https://tools.kinds.or.kr/search/news"

def preprocess_title(title):
    # [단독], (종합) 등 제거 및 특수문자 처리
    title = re.sub(r'\[.*?\]', '', title)  # [ ] 내용 제거
    title = re.sub(r'\(.*?\)', '', title)  # ( ) 내용 제거
    title = title.replace("'", "").strip()  # 특수문자 제거
    title = title.replace('"', "").strip()
    title = title.replace('‘', "").strip()
    title = title.replace('’', "").strip()
    title = title.replace('“', "").strip()
    title = title.replace('”', "").strip()
    return title

def get_news_content_by_title(title):
    # Replace with your actual access key
    access_key = "e8cf85e5-a3ce-467e-8187-96183dbe6bf7"
    
    # Request payload
    payload = {
        "access_key": access_key,
        "argument": {
            "query": title,  # Search for the news by title
            "published_at": {
                "from": "2000-10-31",  # Start date
                "until": "2024-11-28"  # End date
            },
            "fields": ["content", "title", "provider"],  # Specify fields you need
            "return_size": 1  # Limit to the first result
        }
    }
    
    try:
        # Make the POST request
        response = requests.post(url, json=payload)
        response.raise_for_status()
        
        # Parse response
        data = response.json()
        if data["result"] == 0 and "documents" in data["return_object"]:
            documents = data["return_object"]["documents"]
            if documents:
                content = documents[0].get("content", "Content not available")
                return content
            else:
                return "No matching news article found."
        else:
            return f"Error in response: {data}"
    except Exception as e:
        return f"An error occurred: {str(e)}"

def process_and_save_data_row_by_row():
    # 입력 파일 읽기
    input_file = "C:\\Users\\강예린\\Downloads\\sentiment_analysis_output.xlsx"
    output_file = "C:\\Users\\강예린\\본문.xlsx"
    
    # 입력 데이터프레임 읽기
    df = pd.read_excel(input_file)
    
    # 결과 엑셀 파일 초기화 (처음 실행 시)
    try:
        workbook = load_workbook(output_file)
    except FileNotFoundError:
        workbook = Workbook()
        sheet = workbook.active
        sheet.append(["Title", "Content"])  # 헤더 추가
        workbook.save(output_file)
    
    # 기존 파일 불러오기
    workbook = load_workbook(output_file)
    sheet = workbook.active

    # 데이터 처리 및 저장
    for index, row in df.iterrows():
        title = row["Title"]
        preprocessed_title = preprocess_title(title)
        content = get_news_content_by_title(preprocessed_title)
        
        # 결과를 바로 엑셀에 추가
        sheet.append([title, content])
        workbook.save(output_file)  # 행 단위로 저장
        print(f"Saved: {title} {index}")

    print(f"모든 데이터를 처리하고 {output_file}에 저장 완료")

# 실행
process_and_save_data_row_by_row()
