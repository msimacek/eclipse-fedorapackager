Name: example
Version: 1
Release: 1%{?dist}
Summary: This is an example package	
License: GPL
URL: www.example.com			
Source0: example-1.tar.gz
BuildArch: noarch

%description
Example package.

%prep
%setup -q

%build
mkdir build
pushd build
  gcc -o example.out ../example.c
popd

%install


%clean
rm -rf %{buildroot}

%files
%defattr(-,root,root,-)
%doc

%changelog

