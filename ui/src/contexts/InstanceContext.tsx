import {createContext, useContext} from 'react';

interface InstanceConfig {
  isSearch: boolean;
  path: string;
  data: any
}

export const InstanceContext = createContext<InstanceConfig>(undefined as unknown as InstanceConfig);


export function useInstance() {
  const ctx = useContext(InstanceContext);
  if (!ctx) throw new Error('useInstance must be used within a InstanceContext');
  return ctx;
}