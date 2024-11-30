import os
import json
import argparse

# 포함할 파일 확장자
INCLUDE_CONTENT_EXTENSIONS = {'.java'}

def build_tree_with_contents(root_dir):
    """
    주어진 루트 디렉토리의 트리 구조를 딕셔너리 형태로 생성합니다.
    특정 확장자의 파일은 내용도 포함합니다.
    """
    tree = {}
    for dirpath, dirnames, filenames in os.walk(root_dir):
        rel_path = os.path.relpath(dirpath, root_dir)
        if rel_path == '.':
            current = tree
        else:
            current = tree
            for part in rel_path.split(os.sep):
                current = current.setdefault(part, {})

        current_files = {}
        for file in filenames:
            file_ext = os.path.splitext(file)[1].lower()
            if file_ext in INCLUDE_CONTENT_EXTENSIONS:
                file_path = os.path.join(dirpath, file)
                try:
                    with open(file_path, 'r', encoding='utf-8') as f:
                        content = f.read()
                    current_files[file] = content
                except Exception as e:
                    current_files[file] = f"Error reading file: {e}"
            else:
                # 내용 포함하지 않는 파일은 None으로 설정하거나 파일 이름만 저장
                current_files[file] = None
        if current_files:
            current['__files__'] = current_files
    return tree

def save_json(data, output_file):
    """
    데이터를 JSON 파일로 저장합니다.
    """
    with open(output_file, 'w', encoding='utf-8') as f:
        json.dump(data, f, ensure_ascii=False, indent=4)

def main(src_directory, output_file):
    if not os.path.isdir(src_directory):
        print(f"에러: '{src_directory}' 디렉토리가 존재하지 않습니다.")
        return

    tree = build_tree_with_contents(src_directory)
    save_json(tree, output_file)
    print(f"디렉토리 구조가 '{output_file}'에 저장되었습니다.")

if __name__ == "__main__":
    parser = argparse.ArgumentParser(description="Next.js src 디렉토리 구조를 JSON 파일로 저장합니다. 특정 파일 확장자의 경우 코드 내용을 포함합니다.")
    parser.add_argument(
        "--src",
        type=str,
        default="./",
        help="탐색할 src 디렉토리 경로 (기본값: src)"
    )
    parser.add_argument(
        "--output",
        type=str,
        default="directory_structure.json",
        help="저장할 JSON 파일 이름 (기본값: directory_structure.json)"
    )

    args = parser.parse_args()
    main(args.src, args.output)