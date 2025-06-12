package jp.alhinc.calculate_sales;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CalculateSales {

	// 支店定義ファイル名
	private static final String FILE_NAME_BRANCH_LST = "branch.lst";

	// 支店別集計ファイル名
	private static final String FILE_NAME_BRANCH_OUT = "branch.out";

	// エラーメッセージ
	private static final String UNKNOWN_ERROR = "予期せぬエラーが発生しました";
	private static final String FILE_NOT_EXIST = "支店定義ファイルが存在しません";
	private static final String FILE_INVALID_FORMAT = "支店定義ファイルのフォーマットが不正です";
	private static final String FILE_NOT_SERIALNUMBER = "売上ファイル名が連番になっていません";
	private static final String FILE_OVER_DIGITS = "合計金額が10桁を超えました";
	private static final String FILE_NOT_STORECODE = "の支店コードが不正です";
	private static final String FILE_INVALID_FORMATE_RCDFILES = "のフォーマットが不正です";

	/**
	 * メインメソッド
	 *
	 * @param コマンドライン引数
	 */
	public static void main(String[] args) {
		// 支店コードと支店名を保持するMap
		Map<String, String> branchNames = new HashMap<>();
		// 支店コードと売上金額を保持するMap
		Map<String, Long> branchSales = new HashMap<>();

		//コマンドライン引数が渡されていない場合は、エラーメッセージをコンソールに表示します(エラー処理3-1)
		if (args.length != 1) {
			System.err.println("予期せぬエラーが発生しました");
		}

		// 支店定義ファイル読み込み処理
		if (!readFile(args[0], FILE_NAME_BRANCH_LST, branchNames, branchSales)) {
			return;
		}

		// ※ここから集計処理を作成してください。(処理内容2-1、2-2)
		File[] files = new File(args[0]).listFiles();

		List<File> rcdFiles = new ArrayList<File>();

		//ファイル名を正規表現でチェック、売上ファイルをリストに格納
		for (int i = 0; i < files.length; i++) {
			if (files[i].getName().matches("^[0-9]{8}[.]rcd$")) {
				//リストに格納
				rcdFiles.add(files[i]);
			}
		}

		Collections.sort(rcdFiles);

		//連番チェック　rcdFileの要素数分繰り返す
		for (int i = 0; i < rcdFiles.size() - 1; i++) {

			int former = Integer.parseInt(rcdFiles.get(i).getName().substring(0, 8));
			int latter = Integer.parseInt(rcdFiles.get(i + 1).getName().substring(0, 8));

			if ((latter - former) != 1) {
				System.out.println(FILE_NOT_SERIALNUMBER);
				return;
			}
		}

		for (int i = 0; i < rcdFiles.size(); i++) {
			BufferedReader br = null;

			try {
				//売上ファイルを開く
				File file = rcdFiles.get(i);
				FileReader fr = new FileReader(file);
				br = new BufferedReader(fr);
				String line;

				List<String> sales = new ArrayList<String>();
				//売上ファイルを1行ずつ読込
				while ((line = br.readLine()) != null) {
					//リストに格納
					sales.add(line);
				}

				//売上ファイルの中身が2行でなかった場合は、エラーメッセージをコンソールに表示します。(エラー処理2-4)
				if (rcdFiles.size() != 2) {
					System.out.println(file + FILE_INVALID_FORMATE_RCDFILES);
					return;
				}

				/*支店情報を保持しているMapに売上ファイルの支店コードが存在しなかった場合は
				エラーメッセージをコンソールに表示します。(エラー処理2-3)*/
				if (!branchNames.containsKey(sales.get(0))) {
					System.out.println(file + FILE_NOT_STORECODE);
					return;
				}
				/*売上ファイルの売上金額が数字ではなかった場合は
				エラーメッセージをコンソールに表示します。(エラー処理3-2)*/
				if (file.isFile() && !files[i].getName().matches("^[0-9]{8}[.]rcd$")) {
				}
				/*売上ファイルの売上金額が数字ではなかった場合は
				エラーメッセージをコンソールに表示します。(エラー処理3-2)*/
				if (!sales.get(1).matches("^[0-9]*$")) {
					System.out.println(UNKNOWN_ERROR);
					return;
				}

				//型の変換
				long fileSale = Long.parseLong(sales.get(1));
				//売上金額を加算
				Long saleAmount = branchSales.get(sales.get(0)) + fileSale;
				//売上金額が11桁以上の場合エラーメッセージをコンソールに表示します。(エラー処理2-2）
				if (saleAmount >= 10000000000L) {
					System.out.println(FILE_OVER_DIGITS);
					return;
				}
				//売上のMapに保持する
				branchSales.put(sales.get(0), saleAmount);

			} catch (IOException e) {
				System.out.println(UNKNOWN_ERROR);
				return;

			} finally {
				// ファイルを開いている場合
				if (br != null) {
					try {
						// ファイルを閉じる
						br.close();
					} catch (IOException e) {
						System.out.println(UNKNOWN_ERROR);
						return;
					}
				}
			}
		}

		// 支店別集計ファイル書き込み処理
		if (!writeFile(args[0], FILE_NAME_BRANCH_OUT, branchNames, branchSales)) {
			return;
		}

	}

	/**
	 * 支店定義ファイル読み込み処理
	 *
	 * @param フォルダパス
	 * @param ファイル名
	 * @param 支店コードと支店名を保持するMap
	 * @param 支店コードと売上金額を保持するMap
	 * @return 読み込み可否
	 */
	private static boolean readFile(String path, String fileName, Map<String, String> branchNames,
			Map<String, Long> branchSales) {
		BufferedReader br = null;

		try {
			File file = new File(path, fileName);
			//支店定義ファイルが存在しない場合、、コンソールにエラーメッセージを表示します。(エラー処理1）
			if (!file.exists()) {
				System.out.println(FILE_NOT_EXIST);
				return false;
			}
			FileReader fr = new FileReader(file);
			br = new BufferedReader(fr);
			String line;
			// 一行ずつ読み込む

			while ((line = br.readLine()) != null) {
				// ※ここの読み込み処理を変更してください。(処理内容1-2) →済
				String[] items = line.split(",");

				//エラーメッセージをコンソールに表示します。(エラー処理1）
				if ((items.length != 2) || (!items[0].matches("\\d{3}"))) {
					System.out.println(FILE_INVALID_FORMAT);
					return false;
				}

				//Mapに追加する2つの情報をputの引数として指定します。
				branchNames.put(items[0], items[1]);
				branchSales.put(items[0], 0L);
			}

		} catch (IOException e) {
			System.out.println(UNKNOWN_ERROR);
			return false;
		} finally {
			// ファイルを開いている場合
			if (br != null) {
				try {
					// ファイルを閉じる
					br.close();
				} catch (IOException e) {
					System.out.println(UNKNOWN_ERROR);
					return false;
				}
			}
		}
		return true;
	}

	/**
	 * 支店別集計ファイル書き込み処理
	 *
	 * @param フォルダパス
	 * @param ファイル名
	 * @param 支店コードと支店名を保持するMap
	 * @param 支店コードと売上金額を保持するMap
	 * @return 書き込み可否
	 */
	private static boolean writeFile(String path, String fileName, Map<String, String> branchNames,
			Map<String, Long> branchSales) {
		// ※ここに書き込み処理を作成してください。(処理内容3-1)
		BufferedWriter bw = null;

		try {
			File file = new File(path, fileName);

			FileWriter fw = new FileWriter(file);
			bw = new BufferedWriter(fw);

			for (String key : branchSales.keySet()) {
				bw.write(key + "," + branchNames.get(key) + "," + branchSales.get(key));
				bw.newLine();
			}

		} catch (IOException e) {
			System.out.println(UNKNOWN_ERROR);
			return false;
		} finally {
			// ファイルを開いている場合
			if (bw != null) {
				try {
					// ファイルを閉じる
					bw.close();
				} catch (IOException e) {
					System.out.println(UNKNOWN_ERROR);
					return false;
				}
			}
		}
		return true;
	}
}