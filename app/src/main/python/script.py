import pandas as pd
import numpy as np
from sklearn.linear_model import LinearRegression

# url = "https://raw.githubusercontent.com/aliftrd/data-karkas/main/datasapi.csv"
url = "https://raw.githubusercontent.com/aliftrd/data-karkas/main/datasapi_baru.csv"
data = pd.read_csv(url)
TP = np.array(data["X2"])
LD = np.array(data["X3"])
PB = np.array(data["X4"])
TPP = np.array(data["X1"])
Output = np.array(data["X5"])

def main(TPInput, LDInput, PBInput, TPPOutput):
#     varx = []
#     for i in range(len(TP)):
#         varx.append([TP[i], LD[i], PB[i], TPP[i]])
#     dataResult = LinearRegression().fit(varx, Output)
#     result = dataResult.intercept_ + (dataResult.coef_[0] * TPInput) + (dataResult.coef_[1] * LDInput) + (dataResult.coef_[2] * PBInput) + (dataResult.coef_[3] * TPPOutput)
    dataResult = LinearRegression().fit(data.drop(['X5'], axis=1).values, data['X5'].values)
    result = dataResult.predict([[TPInput, LDInput, PBInput, TPPOutput]])
    return result[0]