import pandas as pd
import numpy as np
import xgboost as xgb
from sklearn.model_selection import train_test_split
from sklearn.metrics import mean_squared_error, r2_score, accuracy_score, f1_score
from sklearn.preprocessing import LabelEncoder
import os

def main():
    files = ['FedCycleData071012.csv', 'FedCycleData071012_deriv1.csv', 'FedCycleData071012_deriv2.csv']
    dfs = []
    for f in files:
        if os.path.exists(f):
            dfs.append(pd.read_csv(f))
        else:
            print(f"Warning: {f} not found.")

    if not dfs:
        print("No data files found.")
        return

    df = pd.concat(dfs, ignore_index=True)
    print(f"Combined data shape: {df.shape}")

    # Replace spaces with NaN
    df = df.replace(' ', np.nan)
    df = df.replace('', np.nan)

    # Drop explicitly non-essential/text columns and ID
    drop_cols = ['ClientID', 'Medvitexplain', 'MedvitexplainM', 'Methoddate', 'Whychart', 'Religion', 'Ethnicity', 'OccupationM']
    df = df.drop(columns=[c for c in drop_cols if c in df.columns], errors='ignore')

    targets = ['LengthofCycle', 'EstimatedDayofOvulation', 'CycleWithPeakorNot']

    for target in targets:
        if target not in df.columns:
            continue
            
        print(f"\n{'='*40}")
        print(f"--- Training for Target: {target} ---")
        
        # Drop rows where target is missing
        df_model = df.dropna(subset=[target]).copy()
        
        y = df_model[target]
        # Convert y to numeric
        y = pd.to_numeric(y, errors='coerce')
        
        # Drop rows where y is NaN after conversion
        valid_idx = y.notna()
        y = y[valid_idx]
        df_model = df_model[valid_idx]
        
        # X excludes all target columns to prevent leakage
        X = df_model.drop(columns=targets, errors='ignore')
        
        # Convert all object columns in X to numeric if possible, else label encode
        for col in X.columns:
            # Try to convert to numeric first
            try:
                X[col] = pd.to_numeric(X[col])
            except (ValueError, TypeError):
                pass
            if not pd.api.types.is_numeric_dtype(X[col]):
                X[col] = X[col].astype(str)
                le = LabelEncoder()
                X[col] = le.fit_transform(X[col])
                
        # Train test split
        X_train, X_test, y_train, y_test = train_test_split(X, y, test_size=0.2, random_state=42)
        
        print(f"Training data size: {X_train.shape[0]} samples, {X_train.shape[1]} features.")
        
        if target == 'CycleWithPeakorNot':
            model = xgb.XGBClassifier(eval_metric='logloss', random_state=42)
            model.fit(X_train, y_train)
            y_pred = model.predict(X_test)
            acc = accuracy_score(y_test, y_pred)
            f1 = f1_score(y_test, y_pred, average='weighted')
            print(f"Accuracy: {acc:.4f}")
            print(f"F1-Score: {f1:.4f}")
        else:
            model = xgb.XGBRegressor(random_state=42)
            model.fit(X_train, y_train)
            y_pred = model.predict(X_test)
            rmse = np.sqrt(mean_squared_error(y_test, y_pred))
            r2 = r2_score(y_test, y_pred)
            print(f"RMSE: {rmse:.4f}")
            print(f"R2 Score: {r2:.4f}")
            
        # Feature importance
        importances = model.feature_importances_
        indices = np.argsort(importances)[::-1]
        print("Top 5 important features:")
        for i in range(min(5, len(indices))):
            print(f"  {i+1}. {X.columns[indices[i]]}: {importances[indices[i]]:.4f}")

if __name__ == "__main__":
    main()
