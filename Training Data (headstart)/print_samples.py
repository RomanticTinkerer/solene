import pandas as pd
import numpy as np
import xgboost as xgb
from sklearn.model_selection import train_test_split
from sklearn.preprocessing import LabelEncoder
import os

def main():
    files = ['FedCycleData071012.csv', 'FedCycleData071012_deriv1.csv', 'FedCycleData071012_deriv2.csv']
    dfs = []
    for f in files:
        if os.path.exists(f):
            dfs.append(pd.read_csv(f))
            
    if not dfs:
        print("No data files found.")
        return

    df = pd.concat(dfs, ignore_index=True)

    # Replace spaces with NaN
    df = df.replace(' ', np.nan)
    df = df.replace('', np.nan)

    # Drop explicitly non-essential/text columns and ID
    drop_cols = ['ClientID', 'Medvitexplain', 'MedvitexplainM', 'Methoddate', 'Whychart', 'Religion', 'Ethnicity', 'OccupationM']
    df = df.drop(columns=[c for c in drop_cols if c in df.columns], errors='ignore')

    target = 'LengthofCycle'
    
    df_model = df.dropna(subset=[target]).copy()
    y = df_model[target]
    y = pd.to_numeric(y, errors='coerce')
    valid_idx = y.notna()
    y = y[valid_idx]
    df_model = df_model[valid_idx]
    
    # We will pick a few important features to show in the table so it's not too wide
    targets = ['LengthofCycle', 'EstimatedDayofOvulation', 'CycleWithPeakorNot']
    X = df_model.drop(columns=targets, errors='ignore')
    
    for col in X.columns:
        try:
            X[col] = pd.to_numeric(X[col])
        except (ValueError, TypeError):
            pass
        if not pd.api.types.is_numeric_dtype(X[col]):
            X[col] = X[col].astype(str)
            le = LabelEncoder()
            X[col] = le.fit_transform(X[col])
            
    X_train, X_test, y_train, y_test = train_test_split(X, y, test_size=0.2, random_state=42)
    
    model = xgb.XGBRegressor(random_state=42)
    model.fit(X_train, y_train)
    y_pred = model.predict(X_test)
    
    # Select 5 samples from the test set
    sample_indices = range(5)
    
    print("| Age | BMI | Total Day of Fertility | **Predicted Length of Cycle** | **Actual Length of Cycle** |")
    print("|-----|-----|------------------------|-------------------------------|----------------------------|")
    
    for i in sample_indices:
        idx = X_test.index[i]
        age = X_test.loc[idx, 'Age'] if 'Age' in X_test.columns else 'N/A'
        bmi = X_test.loc[idx, 'BMI'] if 'BMI' in X_test.columns else 'N/A'
        fert_days = X_test.loc[idx, 'TotalDaysofFertility'] if 'TotalDaysofFertility' in X_test.columns else 'N/A'
        
        pred_val = f"{y_pred[i]:.2f}"
        actual_val = f"{y_test.iloc[i]}"
        
        # Round BMI if numeric
        if isinstance(bmi, (int, float)) and not pd.isna(bmi):
            bmi = f"{bmi:.1f}"
        
        print(f"| {age} | {bmi} | {fert_days} | {pred_val} | {actual_val} |")

if __name__ == "__main__":
    main()
