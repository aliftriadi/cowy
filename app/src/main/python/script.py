import pandas as pd
import numpy as np
from sklearn.linear_model import LinearRegression

url = "https://raw.githubusercontent.com/aliftriadi/karkas-sapi/main/data_sapi.csv"
data = pd.read_csv(url)

TP = np.array(data["tinggi_pundak"])
LD = np.array(data["lingkar_dada"])
PB = np.array(data["panjang_badan"])
TPP = np.array(data["tinggi_pinggul"])
Output = np.array(data["bobot"])

def main(TPInput, LDInput, PBInput, TPPOutput):
    varx = []
    for i in range(len(TP)):
        varx.append([TP[i], LD[i], PB[i], TPP[i]])

    print(varx)
    dataResult = LinearRegression().fit(varx, Output)
    result = dataResult.intercept_ + (dataResult.coef_[0] * TPInput) + (dataResult.coef_[1] * LDInput) + (dataResult.coef_[2] * PBInput) + (dataResult.coef_[3] * TPPOutput)
    return result